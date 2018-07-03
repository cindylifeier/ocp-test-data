package gov.samhsa.ocp.ocpfis;

import gov.samhsa.ocp.ocpfis.model.appointment.TempAppointmentDto;
import gov.samhsa.ocp.ocpfis.service.dto.AppointmentParticipantDto;
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
public class AppointmentsHelper {

    public static void process(Sheet appointments, Map<String, String> mapOfPatients, Map<String, String> mapOfPractitioners) {
        log.info("last row number : "+ appointments.getLastRowNum());

        int rowNum=0;
        Map<String,ValueSetDto> statusValueSetLookups= CommonHelper.getLookupValueSet(DataConstants.serverUrl + "lookups/appointment-statuses");
        Map<String,String> typeCodeLookups=CommonHelper.getLookup(DataConstants.serverUrl + "lookups/appointment-types");
        Map<String,ValueSetDto> participationTypes=CommonHelper.getLookupValueSet(DataConstants.serverUrl + "lookups/appointment-participation-types");
        Map<String,ValueSetDto> participationStatus=CommonHelper.getLookupValueSet(DataConstants.serverUrl + "lookups/appointment-participation-statuses");
        Map<String,ValueSetDto> participantRequired=CommonHelper.getLookupValueSet(DataConstants.serverUrl + "lookups/appointment-participant-required");
        List<TempAppointmentDto> appointmentDtos = retrieveSheet(appointments, mapOfPatients, mapOfPractitioners, rowNum, typeCodeLookups, participationTypes, participationStatus, participantRequired);
        RestTemplate rt=new RestTemplate();

        appointmentDtos.forEach(appointmentDto -> {
            log.info("appointments : "+appointmentDto);
            try {
                HttpEntity<TempAppointmentDto> request = new HttpEntity<>(appointmentDto);

                rt.postForObject(DataConstants.serverUrl + "appointments", request, TempAppointmentDto.class);
            }catch (Exception e){
                log.info("This appointments counld not be posted: "+appointmentDto);
                e.printStackTrace();
            }
        });

    }

    private static List<TempAppointmentDto> retrieveSheet(Sheet appointments, Map<String, String> mapOfPatients, Map<String, String> mapOfPractitioners, int rowNum, Map<String, String> typeCodeLookups, Map<String, ValueSetDto> participationTypes, Map<String, ValueSetDto> participationStatus, Map<String, ValueSetDto> participantRequired) {
        List<TempAppointmentDto> appointmentDtos=new ArrayList<>();
        AppointmentParticipantDto appointmentParticipantDto=new AppointmentParticipantDto();
        for(Row row:appointments){
            if(rowNum>0){
                int j=0;
                TempAppointmentDto dto=new TempAppointmentDto();
                List<AppointmentParticipantDto> participantDtos=new ArrayList<>();
                AppointmentParticipantDto patientParticipant=new AppointmentParticipantDto();
                AppointmentParticipantDto otherParticipant=new AppointmentParticipantDto();

                String date=null;
                try {
                    processRow(mapOfPatients, mapOfPractitioners, typeCodeLookups, participationTypes, participationStatus, participantRequired, row, j, dto, participantDtos, patientParticipant, otherParticipant, date);
                } catch (Exception e) {
                    log.error("Error processing a row for Appointments");
                }
                appointmentDtos.add(dto);
            }
            rowNum++;
        }
        return appointmentDtos;
    }

    private static void processRow(Map<String, String> mapOfPatients, Map<String, String> mapOfPractitioners, Map<String, String> typeCodeLookups, Map<String, ValueSetDto> participationTypes, Map<String, ValueSetDto> participationStatus, Map<String, ValueSetDto> participantRequired, Row row, int j, TempAppointmentDto dto, List<AppointmentParticipantDto> participantDtos, AppointmentParticipantDto patientParticipant, AppointmentParticipantDto otherParticipant, String date) {
        for(Cell cell:row){
            String cellValue=new DataFormatter().formatCellValue(cell);

            if(j==0){
                patientParticipant.setActorName(cellValue);
                patientParticipant.setActorReference("Patient/"+mapOfPatients.get(cellValue));
                participantDtos.add(patientParticipant);
            } else if(j==1){
                dto.setDescription(cellValue);
            } else if(j==2){
                date=cellValue;
            } else if(j==3){
                dto.setStart(date+"T"+cellValue);
            }else if(j==4){
                dto.setEnd(date+"T"+cellValue);
            }else if(j==5){
                dto.setTypeCode(typeCodeLookups.get(cellValue));
            }else if(j==6){
                otherParticipant.setActorReference("Practitioner/"+mapOfPractitioners.get(cellValue));
                otherParticipant.setActorName(cellValue);
            }else if(j==7){
                otherParticipant.setParticipationTypeCode(participationTypes.get(cellValue).getCode());
                otherParticipant.setParticipationTypeSystem(participationTypes.get(cellValue).getSystem());
                otherParticipant.setParticipationTypeDisplay(participationTypes.get(cellValue).getDisplay());
            }else if(j==8){
                otherParticipant.setParticipationStatusCode(participationStatus.get(cellValue).getCode());
                otherParticipant.setParticipationStatusSystem(participationStatus.get(cellValue).getSystem());
                otherParticipant.setParticipationStatusDisplay(participationStatus.get(cellValue).getDisplay());
            }else if(j==9){
                otherParticipant.setParticipantRequiredCode(participantRequired.get(cellValue).getCode());
                otherParticipant.setParticipantRequiredSystem(participantRequired.get(cellValue).getSystem());
                otherParticipant.setParticipantRequiredDisplay(participantRequired.get(cellValue).getDisplay());
                participantDtos.add(otherParticipant);
                dto.setParticipant(participantDtos);
            }else if(j==10){
                dto.setCreatorName(cellValue);
                dto.setCreatorReference("Practitioner/"+mapOfPractitioners.get(cellValue));
            }
            j++;
        }
    }
}
