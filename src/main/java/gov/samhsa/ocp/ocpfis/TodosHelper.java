package gov.samhsa.ocp.ocpfis;

import gov.samhsa.ocp.ocpfis.model.activitydefinition.TempPeriodDto;
import gov.samhsa.ocp.ocpfis.model.task.TempTaskDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class TodosHelper {

    public static void process(Sheet todos, Map<String,String> mapOfPatient,Map<String,String> mapOfOrganizations) {

        int rowNum=0;

        List<TempTaskDto> todoDtos = retrieveSheet(todos, mapOfPatient, mapOfOrganizations, rowNum);
        RestTemplate rt=new RestTemplate();

        todoDtos.forEach(taskDto->{
            try {
                log.info("tasks  : " + taskDto);
                HttpEntity<TempTaskDto> request = new HttpEntity<>(taskDto);

                rt.postForObject(DataConstants.serverUrl + "tasks", request, TempTaskDto.class);
            } catch (Exception e) {
                log.error("Todo could not be posted");

            }
        });
    }

    private static List<TempTaskDto> retrieveSheet(Sheet todos, Map<String, String> mapOfPatient, Map<String, String> mapOfOrganizations, int rowNum) {
        List<TempTaskDto> todoDtos=new ArrayList<>();
        Map<String,ValueSetDto> statusLookupValueSet= CommonHelper.getLookupValueSet(DataConstants.serverUrl + "lookups/task-status");
        Map<String,ValueSetDto> priorityValueSet=CommonHelper.getLookupValueSet(DataConstants.serverUrl + "lookups/request-priority");
        Map<String,ValueSetDto> intentValueSet=CommonHelper.getLookupValueSet(DataConstants.serverUrl + "lookups/request-intent");
        Map<String,ValueSetDto> performerValueSet=CommonHelper.getLookupValueSet(DataConstants.serverUrl + "lookups/task-performer-type");
        for(Row row: todos){
            if(rowNum>0){
                int j=0;
                TempTaskDto dto=new TempTaskDto();
                TempPeriodDto tempPeriodDto=new TempPeriodDto();
                try {
                    processRow(mapOfPatient, mapOfOrganizations, statusLookupValueSet, priorityValueSet, intentValueSet, performerValueSet, row, j, dto, tempPeriodDto);
                    todoDtos.add(dto);
                } catch (Exception e) {
                    log.error("Error processing row of a TODO");
                }

            }
            rowNum++;
        }
        return todoDtos;
    }

    private static void processRow(Map<String, String> mapOfPatient, Map<String, String> mapOfOrganizations, Map<String, ValueSetDto> statusLookupValueSet, Map<String, ValueSetDto> priorityValueSet, Map<String, ValueSetDto> intentValueSet, Map<String, ValueSetDto> performerValueSet, Row row, int j, TempTaskDto dto, TempPeriodDto tempPeriodDto) {
        for(Cell cell: row){
            String cellValue=new DataFormatter().formatCellValue(cell);

            if(j==0){
                ReferenceDto referenceDto=new ReferenceDto();
                referenceDto.setReference("Patient/"+mapOfPatient.get(cellValue));
                referenceDto.setDisplay(cellValue);
                dto.setBeneficiary(referenceDto);
            }
            else if(j==1){
                dto.setDescription(cellValue);
            }else if(j==2){
                ReferenceDto referenceDto=new ReferenceDto();
                referenceDto.setReference("Organization/"+mapOfOrganizations.get(cellValue));
                referenceDto.setDisplay(cellValue);
                dto.setOrganization(referenceDto);

            }else if(j==3){
                dto.setStatus(statusLookupValueSet.get(cellValue));
            }else if(j==4){
                dto.setPriority(priorityValueSet.get(cellValue));
            }else if(j==5) {
                dto.setIntent(intentValueSet.get(cellValue));
            }else if(j==6){
                ReferenceDto referenceDto=new ReferenceDto();
                referenceDto.setReference("Practitioner/"+ CommonHelper.getPractitionerId(cellValue.trim().split(" ")[1]));
                referenceDto.setDisplay(cellValue);
                dto.setAgent(referenceDto);
            }else if(j==7){
                ReferenceDto referenceDto=new ReferenceDto();
                referenceDto.setReference("Practitioner/"+CommonHelper.getPractitionerId(cellValue.trim().split(" ")[0]));
                referenceDto.setDisplay(cellValue);
                dto.setOwner(referenceDto);
            }else if(j==8){
                dto.setPerformerType(performerValueSet.get(cellValue));
            }else if(j==9){
                tempPeriodDto.setStart(cellValue);
            }else if(j==10){
                tempPeriodDto.setEnd(cellValue);
                dto.setExecutionPeriod(tempPeriodDto);
            }else if(j==11){
                ReferenceDto referenceDto=new ReferenceDto();
                referenceDto.setReference("ActivityDefinition/"+CommonHelper.getActivityDefinitionId(dto.getOrganization().getReference().split("/")[1],cellValue));
                referenceDto.setDisplay(cellValue);
                dto.setDefinition(referenceDto);

                ReferenceDto partOfReference=new ReferenceDto();
                partOfReference.setDisplay("To-Do");
                partOfReference.setReference(CommonHelper.getTodoMainTask(dto.getBeneficiary().getReference().split("/")[1],dto.getOrganization().getReference().split("/")[1]));
                dto.setPartOf(partOfReference);
            }
            j++;
        }
    }
}
