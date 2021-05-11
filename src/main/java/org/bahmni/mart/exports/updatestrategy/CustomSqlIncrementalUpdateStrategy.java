package org.bahmni.mart.exports.updatestrategy;

import org.bahmni.mart.config.job.JobDefinitionReader;
import org.bahmni.mart.config.job.model.JobDefinition;
import org.bahmni.mart.table.SpecialCharacterResolver;
import org.bahmni.mart.table.domain.TableData;
import org.bahmni.mart.table.listener.AbstractJobListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;


@Component
public class CustomSqlIncrementalUpdateStrategy extends AbstractIncrementalUpdateStrategy {

    private static final String UPDATED_READER_SQL = "SELECT * FROM ( %s ) result WHERE %s IN (%s)";

    @Autowired
    private JobDefinitionReader jobDefinitionReader;

    @Qualifier("openmrsJdbcTemplate")
    @Autowired
    private JdbcTemplate openmrsJdbcTemplate;

    @Override
    public boolean getMetaDataChangeStatus(String tableName, String jobName) {
        JobDefinition jobDefinition = jobDefinitionReader.getJobDefinitionByName(jobName);
        if (isEmpty(jobDefinition.getName()) || isNull(jobDefinition.getIncrementalUpdateConfig()))
            return true;

        TableData tableData = listener.getTableDataForMart(jobDefinition);
        SpecialCharacterResolver.resolveTableData(tableData);
        return !tableData.equals(getExistingTableData(tableName));
    }

    @Override
    public void setListener(AbstractJobListener listener) {
        this.listener = listener;
    }

    @Override
    String getSqlForIncrementalUpdate(String readerSql, String updateOn,
                                      Optional<Map<String, Object>> optionalMarkerMap) {
        String joinedIds = getJoinedIds(optionalMarkerMap.get());
        if (readerSql.contains("orders")) {
            String queryForPreviousOrderEncounterIds = "Select distinct o1.encounter_id from orders o1, orders o2 " +
                    "where o1.order_id = o2.previous_order_id and o2.order_action IN ('DISCONTINUE','REVISE') " +
                    "and o1.order_id != o2.order_id and o2.encounter_id IN (%s)";
            String encounterIdsToAppend = openmrsJdbcTemplate.queryForList(
                    String.format(queryForPreviousOrderEncounterIds, joinedIds), String.class).stream()
                    .collect(Collectors.joining(","));
            if (!encounterIdsToAppend.isEmpty()) {
                joinedIds = String.join(",", joinedIds, encounterIdsToAppend);
            }
        }
        return String.format(UPDATED_READER_SQL, readerSql, updateOn, joinedIds);
    }
}
