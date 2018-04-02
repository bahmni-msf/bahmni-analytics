package org.bahmni.mart.config.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ViewExecutor {

    private static Logger logger = LoggerFactory.getLogger(ViewExecutor.class);

    @Qualifier("martJdbcTemplate")
    @Autowired
    private JdbcTemplate martJdbcTemplate;

    public void execute(List<ViewDefinition> viewDefinitions) {
        viewDefinitions.forEach(viewDefinition -> {
            try {
                logger.info(String.format("Executing the view '%s'.", viewDefinition.getName()));
                martJdbcTemplate.execute(getUpdatedViewSQL(viewDefinition));
            } catch (Exception e) {
                logger.error(String.format("Unable to execute the view %s.", viewDefinition.getName()), e);
            }
        });
    }

    private String getUpdatedViewSQL(ViewDefinition viewDefinition) {
        return String.format("drop view if exists %s;create view %s as %s",
                viewDefinition.getName(), viewDefinition.getName(), viewDefinition.getSql());
    }
}