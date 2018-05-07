package org.bahmni.mart.config;

import org.bahmni.mart.BatchUtils;
import org.bahmni.mart.config.job.JobDefinition;
import org.bahmni.mart.config.procedure.ProcedureDefinition;
import org.bahmni.mart.config.view.ViewDefinition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.List;

import static org.bahmni.mart.CommonTestHelper.setValuesForMemberFields;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(BatchUtils.class)
public class MartJSONReaderTest {

    private MartJSONReader martJSONReader;

    @Mock
    private BahmniMartJSON bahmniMartJSON;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {

        JobDefinition jobDefinition = mock(JobDefinition.class);
        when(jobDefinition.getName()).thenReturn("Program Data");
        when(jobDefinition.getType()).thenReturn("customSql");
        when(jobDefinition.getReaderSql()).thenReturn("select * from program");
        when(jobDefinition.getChunkSizeToRead()).thenReturn(1000);
        when(jobDefinition.getTableName()).thenReturn("MyProgram");

        ViewDefinition viewDefinition = mock(ViewDefinition.class);
        when(viewDefinition.getName()).thenReturn("patient_program_view");
        when(viewDefinition.getSql()).thenReturn("select * from patient_program");

        ProcedureDefinition procedureDefinition = mock(ProcedureDefinition.class);
        when(procedureDefinition.getName()).thenReturn("Test Procedure");
        when(procedureDefinition.getSourceFilePath()).thenReturn("some path");

        bahmniMartJSON = mock(BahmniMartJSON.class);
        when(bahmniMartJSON.getJobs()).thenReturn(Arrays.asList(jobDefinition));
        when(bahmniMartJSON.getViews()).thenReturn(Arrays.asList(viewDefinition));
        when(bahmniMartJSON.getProcedures()).thenReturn(Arrays.asList(procedureDefinition));

        martJSONReader = new MartJSONReader();
        setValuesForMemberFields(martJSONReader, "bahmniMartJSON", bahmniMartJSON);
    }

    @Test
    public void shouldReturnJobDefinitions() {
        List<JobDefinition> actualJobDefinitions = martJSONReader.getJobDefinitions();

        assertEquals(1, actualJobDefinitions.size());
        JobDefinition jobDefinition = actualJobDefinitions.get(0);
        assertEquals("Program Data", jobDefinition.getName());
        assertEquals("customSql", jobDefinition.getType());
        assertEquals("select * from program", jobDefinition.getReaderSql());
        assertEquals(1000, jobDefinition.getChunkSizeToRead());
        assertEquals("MyProgram", jobDefinition.getTableName());

        verify(bahmniMartJSON, times(1)).getJobs();
    }

    @Test
    public void shouldReturnViewDefinitions() {
        List<ViewDefinition> viewDefinitions = martJSONReader.getViewDefinitions();

        assertEquals(1, viewDefinitions.size());
        ViewDefinition viewDefinition = viewDefinitions.get(0);
        assertEquals("patient_program_view", viewDefinition.getName());
        assertEquals("select * from patient_program", viewDefinition.getSql());

        verify(bahmniMartJSON, times(1)).getViews();
    }

    @Test
    public void shouldReturnProcedureDefinitions() throws Exception {
        List<ProcedureDefinition> procedureDefinitions = martJSONReader.getProcedureDefinitions();

        assertEquals(1, procedureDefinitions.size());
        ProcedureDefinition procedureDefinition = procedureDefinitions.get(0);
        assertEquals("Test Procedure", procedureDefinition.getName());
        assertEquals("some path", procedureDefinition.getSourceFilePath());

        verify(bahmniMartJSON, times(1)).getProcedures();
    }

    @Test
    public void shouldGiveEmptyListAsJobDefinitionsIfJobsAreNotPresent() throws Exception {
        BahmniMartJSON spyMartJson = spy(new BahmniMartJSON());
        setValuesForMemberFields(martJSONReader, "bahmniMartJSON", spyMartJson);

        assertTrue(martJSONReader.getJobDefinitions().isEmpty());
        verify(spyMartJson, times(1)).getJobs();
    }

    @Test
    public void shouldGiveEmptyListAsViewsIfViewsAreNotPresentInConfig() throws Exception {
        BahmniMartJSON spyMartJson = spy(new BahmniMartJSON());
        setValuesForMemberFields(martJSONReader, "bahmniMartJSON", spyMartJson);

        assertTrue(martJSONReader.getViewDefinitions().isEmpty());
        verify(spyMartJson, times(1)).getViews();
    }

    @Test
    public void shouldGiveEmptyListAsProceduresGivenNoProceduresInConfig() throws Exception {
        BahmniMartJSON spyMartJson = spy(new BahmniMartJSON());
        setValuesForMemberFields(martJSONReader, "bahmniMartJSON", spyMartJson);

        assertTrue(martJSONReader.getProcedureDefinitions().isEmpty());
        verify(spyMartJson, times(1)).getProcedures();
    }

    @Test
    public void shouldReadJSONForFirstTime() throws NoSuchFieldException, IllegalAccessException {
        mockStatic(BatchUtils.class);
        String json = "{\n" +
                "  \"jobs\": [\n" +
                "    {\n" +
                "      \"name\": \"test name\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        when(BatchUtils.convertResourceOutputToString(any())).thenReturn(json);
        setValuesForMemberFields(martJSONReader, "bahmniMartJSON", null);

        martJSONReader.read();

        verifyStatic(times(1));
        BatchUtils.convertResourceOutputToString(any());

        assertEquals(1, martJSONReader.getJobDefinitions().size());
    }

    @Test
    public void shouldNotReadJSONIfItIsReadAlready() {
        mockStatic(BatchUtils.class);

        martJSONReader.read();

        verifyStatic(times(0));
        BatchUtils.convertResourceOutputToString(any());
    }
}