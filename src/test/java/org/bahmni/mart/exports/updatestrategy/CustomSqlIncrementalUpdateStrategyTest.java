package org.bahmni.mart.exports.updatestrategy;

import org.bahmni.mart.config.job.JobDefinitionReader;
import org.bahmni.mart.config.job.JobDefinitionUtil;
import org.bahmni.mart.config.job.model.IncrementalUpdateConfig;
import org.bahmni.mart.config.job.model.JobDefinition;
import org.bahmni.mart.helper.MarkerManager;
import org.bahmni.mart.helper.TableDataGenerator;
import org.bahmni.mart.table.SpecialCharacterResolver;
import org.bahmni.mart.table.domain.TableData;
import org.bahmni.mart.table.listener.AbstractJobListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static org.bahmni.mart.CommonTestHelper.setValuesForMemberFields;
import static org.bahmni.mart.CommonTestHelper.setValuesForSuperClassMemberFields;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

// NOT WRITING MORE TESTS AS COMMON METHODS ARE COVERED IN ObsIncrementalUpdateStrategyTest.class

@RunWith(PowerMockRunner.class)
@PrepareForTest({JobDefinitionUtil.class, SpecialCharacterResolver.class})
public class CustomSqlIncrementalUpdateStrategyTest {

    private static final String JOB_NAME = "job name";
    private static final String TABLE_NAME = "table name";
    private static final String QUERY_FOR_EVENT_URLS = "SELECT DISTINCT object " +
            "FROM event_records WHERE id > %s AND id <= %s AND binary category = '%s'";
    private static final String QUERY_FOR_ID_EXTRACTION = "SELECT %s_id FROM %s WHERE uuid in (%s)";
    private static final String queryForPreviousOrderEncounterIds = "Select distinct o1.encounter_id " +
            "from orders o1, orders o2 where o1.order_id = o2.previous_order_id and " +
            "o2.order_action IN ('DISCONTINUE','REVISE') " +
            "and o1.order_id != o2.order_id and o2.encounter_id IN (%s)";
    private static final String eventRecordedForJob = "10";
    private static final String MAX_EVENT_RECORD_ID = "20";
    private static final String CATEGORY = "CategoryName";
    private static final String UPDATED_READER_SQL = "SELECT * FROM ( %s ) result WHERE %s IN (%s)";
    private static final String READER_SQL = "SELECT id, name FROM test";
    private static final String ORDERS_READER_SQL = "SELECT id, name FROM orders";
    private static final String UPDATE_ON = "id";

    @Mock
    private JobDefinitionReader jobDefinitionReader;

    @Mock
    private JobDefinition jobDefinition;

    @Mock
    private TableData existingTableData;

    @Mock
    private TableData tableData;

    @Mock
    private TableDataGenerator tableDataGenerator;

    @Mock
    private AbstractJobListener listener;

    @Mock
    private JdbcTemplate openmrsJdbcTemplate;

    @Mock
    private MarkerManager markerManager;

    private CustomSqlIncrementalUpdateStrategy spyCustomSqlIncrementalUpdater;


    @Before
    public void setUp() throws Exception {
        mockStatic(JobDefinitionUtil.class);
        mockStatic(SpecialCharacterResolver.class);

        CustomSqlIncrementalUpdateStrategy customSqlIncrementalUpdater = new CustomSqlIncrementalUpdateStrategy();
        setValuesForMemberFields(customSqlIncrementalUpdater, "jobDefinitionReader", jobDefinitionReader);
        setValuesForMemberFields(customSqlIncrementalUpdater, "openmrsJdbcTemplate", openmrsJdbcTemplate);
        setValuesForSuperClassMemberFields(customSqlIncrementalUpdater, "openmrsJdbcTemplate", openmrsJdbcTemplate);
        setValuesForSuperClassMemberFields(customSqlIncrementalUpdater, "tableDataGenerator", tableDataGenerator);
        setValuesForSuperClassMemberFields(customSqlIncrementalUpdater, "markerManager", markerManager);
        setValuesForSuperClassMemberFields(customSqlIncrementalUpdater, "maxEventRecordId",
                Integer.parseInt(MAX_EVENT_RECORD_ID));
        spyCustomSqlIncrementalUpdater = spy(customSqlIncrementalUpdater);
        spyCustomSqlIncrementalUpdater.setListener(listener);

        when(jobDefinition.getName()).thenReturn(JOB_NAME);
        when(jobDefinition.getTableName()).thenReturn(TABLE_NAME);
        when(jobDefinition.getIncrementalUpdateConfig()).thenReturn(new IncrementalUpdateConfig());
        String readerSql = "select * from table";
        when(JobDefinitionUtil.getReaderSQL(jobDefinition)).thenReturn(readerSql);
        when(tableDataGenerator.getTableDataFromOpenmrs(TABLE_NAME, readerSql)).thenReturn(tableData);
        when(listener.getTableDataForMart(jobDefinition)).thenReturn(tableData);
        when(jobDefinitionReader.getJobDefinitionByName(JOB_NAME)).thenReturn(jobDefinition);
    }

    @Test
    public void shouldReturnTrueIfMetadataChanged() {
        doReturn(existingTableData).when(spyCustomSqlIncrementalUpdater).getExistingTableData(TABLE_NAME);

        boolean status = spyCustomSqlIncrementalUpdater.getMetaDataChangeStatus(TABLE_NAME, JOB_NAME);

        assertTrue(status);
        verify(jobDefinitionReader).getJobDefinitionByName(JOB_NAME);
        verify(jobDefinition, atLeastOnce()).getName();
        verify(jobDefinition).getIncrementalUpdateConfig();
        verify(spyCustomSqlIncrementalUpdater).getExistingTableData(TABLE_NAME);
        verifyStatic(never());
        JobDefinitionUtil.getReaderSQL(jobDefinition);
        verify(listener).getTableDataForMart(jobDefinition);
        verifyStatic();
        SpecialCharacterResolver.resolveTableData(tableData);
    }

    @Test
    public void shouldReturnFalseIfMetadataIsSame() {
        doReturn(tableData).when(spyCustomSqlIncrementalUpdater).getExistingTableData(TABLE_NAME);

        boolean status = spyCustomSqlIncrementalUpdater.getMetaDataChangeStatus(TABLE_NAME, JOB_NAME);

        assertFalse(status);
        verify(jobDefinitionReader).getJobDefinitionByName(JOB_NAME);
        verify(jobDefinition, atLeastOnce()).getName();
        verify(jobDefinition).getIncrementalUpdateConfig();
        verify(spyCustomSqlIncrementalUpdater).getExistingTableData(TABLE_NAME);
        verifyStatic(never());
        JobDefinitionUtil.getReaderSQL(jobDefinition);
        verify(listener).getTableDataForMart(jobDefinition);
        verifyStatic();
        SpecialCharacterResolver.resolveTableData(tableData);
    }

    @Test
    public void shouldReturnTrueIfJobDefinitionNameIsEmpty() {
        when(jobDefinition.getName()).thenReturn("");

        boolean status = spyCustomSqlIncrementalUpdater.getMetaDataChangeStatus(TABLE_NAME, JOB_NAME);

        assertTrue(status);
        verify(jobDefinitionReader).getJobDefinitionByName(JOB_NAME);
        verify(jobDefinition, atLeastOnce()).getName();
        verify(jobDefinition, never()).getIncrementalUpdateConfig();
        verify(spyCustomSqlIncrementalUpdater, never()).getExistingTableData(TABLE_NAME);
        verifyStatic(never());
        JobDefinitionUtil.getReaderSQL(jobDefinition);
        verify(listener, never()).getTableDataForMart(anyString());
    }

    @Test
    public void shouldReturnTrueIfIncrementalUpdateConfigIsNull() {
        when(jobDefinition.getIncrementalUpdateConfig()).thenReturn(null);

        boolean status = spyCustomSqlIncrementalUpdater.getMetaDataChangeStatus(TABLE_NAME, JOB_NAME);

        assertTrue(status);
        verify(jobDefinitionReader).getJobDefinitionByName(JOB_NAME);
        verify(jobDefinition, atLeastOnce()).getName();
        verify(jobDefinition).getIncrementalUpdateConfig();
        verify(spyCustomSqlIncrementalUpdater, never()).getExistingTableData(TABLE_NAME);
        verifyStatic(never());
        JobDefinitionUtil.getReaderSQL(jobDefinition);
        verify(listener, never()).getTableDataForMart(anyString());
    }

    private void setUpForIncrementalUpdateSql() {
        Map markerMap = mock(Map.class);
        when(markerMap.get("event_record_id")).thenReturn(eventRecordedForJob);
        when(markerMap.get("category")).thenReturn(CATEGORY);
        when(markerMap.get("table_name")).thenReturn(TABLE_NAME);
        when(markerManager.getJobMarkerMap(JOB_NAME)).thenReturn(Optional.of(markerMap));

        String queryForEventRecordObjectUrls = format(QUERY_FOR_EVENT_URLS, eventRecordedForJob,
                MAX_EVENT_RECORD_ID, CATEGORY);
        String uuid = "99b885e9-c49c-4b5a-aa4c-e8a8fb93484d";
        when(openmrsJdbcTemplate.queryForList(queryForEventRecordObjectUrls, String.class))
                .thenReturn(Arrays.asList(uuid));
        String queryForIDs = format(QUERY_FOR_ID_EXTRACTION, TABLE_NAME, TABLE_NAME, uuid);
        when(openmrsJdbcTemplate.queryForList(queryForIDs, String.class)).thenReturn(Arrays.asList(""));
    }

    @Test
    public void shouldNotCallPrevEncounterIdsSqlForNonOrdersSqlReaders() {
        setUpForIncrementalUpdateSql();

        String queryForPrvOrderEncounterIds = format(queryForPreviousOrderEncounterIds, "-1");
        when(openmrsJdbcTemplate.queryForList(queryForPrvOrderEncounterIds, String.class))
                .thenReturn(Arrays.asList(""));

        Optional<Map<String, Object>> optionalMarkerMap = spyCustomSqlIncrementalUpdater.getJobMarkerMap(JOB_NAME);
        String actualUpdateReaderSql = spyCustomSqlIncrementalUpdater.getSqlForIncrementalUpdate(READER_SQL,
                UPDATE_ON, optionalMarkerMap);
        String expectedUpdatedReaderSql = format(UPDATED_READER_SQL, READER_SQL, UPDATE_ON, "-1");

        verify(openmrsJdbcTemplate, never()).queryForList(queryForPrvOrderEncounterIds, String.class);
        assertEquals(expectedUpdatedReaderSql, actualUpdateReaderSql);
    }

    @Test
    public void shouldCallPrevEncounterIdsSqlForOrdersSqlReaders() {
        setUpForIncrementalUpdateSql();

        String queryForPrvOrderEncounterIds = format(queryForPreviousOrderEncounterIds, "-1");
        when(openmrsJdbcTemplate.queryForList(queryForPrvOrderEncounterIds, String.class))
                .thenReturn(Arrays.asList(""));

        Optional<Map<String, Object>> optionalMarkerMap = spyCustomSqlIncrementalUpdater.getJobMarkerMap(JOB_NAME);
        String actualUpdateReaderSql = spyCustomSqlIncrementalUpdater.getSqlForIncrementalUpdate(ORDERS_READER_SQL,
                UPDATE_ON, optionalMarkerMap);
        String expectedUpdatedReaderSql = format(UPDATED_READER_SQL, ORDERS_READER_SQL, UPDATE_ON, "-1");

        verify(openmrsJdbcTemplate).queryForList(queryForPrvOrderEncounterIds, String.class);
        assertEquals(expectedUpdatedReaderSql, actualUpdateReaderSql);
    }

    @Test
    public void shouldAppendPrevOrderEncounterIdsInQueryForOrderSqlReaders() {
        setUpForIncrementalUpdateSql();

        String queryForPrvOrderEncounterIds = format(queryForPreviousOrderEncounterIds, "-1");
        when(openmrsJdbcTemplate.queryForList(queryForPrvOrderEncounterIds, String.class))
                .thenReturn(Arrays.asList("1","2"));

        Optional<Map<String, Object>> optionalMarkerMap = spyCustomSqlIncrementalUpdater.getJobMarkerMap(JOB_NAME);
        String actualUpdateReaderSql = spyCustomSqlIncrementalUpdater.getSqlForIncrementalUpdate(ORDERS_READER_SQL,
                UPDATE_ON, optionalMarkerMap);
        String expectedUpdatedReaderSql = format(UPDATED_READER_SQL, ORDERS_READER_SQL, UPDATE_ON, "-1,1,2");

        verify(openmrsJdbcTemplate).queryForList(queryForPrvOrderEncounterIds, String.class);
        assertEquals(expectedUpdatedReaderSql, actualUpdateReaderSql);
    }

}
