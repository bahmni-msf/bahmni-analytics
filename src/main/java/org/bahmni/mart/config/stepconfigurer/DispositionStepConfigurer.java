package org.bahmni.mart.config.stepconfigurer;

import org.bahmni.mart.config.job.JobDefinitionUtil;
import org.bahmni.mart.form.domain.BahmniForm;
import org.bahmni.mart.form.domain.Concept;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class DispositionStepConfigurer  extends StepConfigurer {

    private static final String DISPOSITION_JOB_TYPE = "disposition";
    private static final String DISPOSITION_CONCEPT_NAME = "Disposition Set";

    @Override
    protected List<BahmniForm> getAllForms() {
        List<String> ignoreConcepts = JobDefinitionUtil.getIgnoreConceptNamesForJob(JobDefinitionUtil
                .getJobDefinitionByType(jobDefinitionReader.getJobDefinitions(), DISPOSITION_JOB_TYPE));
        List<Concept> allConcepts = obsService
                .getConceptsByNames(Collections.singletonList(DISPOSITION_CONCEPT_NAME));
        return formListProcessor.retrieveAllForms(allConcepts, ignoreConcepts);
    }
}