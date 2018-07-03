package gov.samhsa.ocp.ocpfis.model.appointment;

import gov.samhsa.ocp.ocpfis.service.dto.AppointmentParticipantDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TempAppointmentDto {
    private String logicalId;

    private String statusCode;
    private String typeCode;

    private String description;
    private String appointmentDate;
    private String appointmentDuration;
    private String patientName;

    private String start;
    private String end;
    private String created;

    private List<ReferenceDto> slot; //To be used later
    private List<ReferenceDto> incomingReferral;//To be used later

    //Used to identify person(Practitioner/Patient) who created the appointment from the UI
    private String creatorReference;
    private String creatorName;

    private List<AppointmentParticipantDto> participant;

    //These help to show the required menu options on the UI
    private boolean canCancel;
    private boolean canAccept;
    private boolean canDecline;
    private boolean canTentativelyAccept;
    private String requesterParticipationStatusCode;
}
