#!/bin/bash

UAA_BASE_URL="http://localhost:8080/uaa"
CLIENT_ID="admin-client"
CLIENT_SECTRET="changeit"
ADMIN_USERNAME="scim-admin"
ADMIN_PASSWORD="AAA!aaa1"
RESPONSE_TYPE="token"
GRANT_TYPE="password"

SMART_ROLE_NAME="ocp.role.smartUser"
SMART_ROLE_DESCRIPTION="ocp smart user role"

ocpSmartLaunchScope=(launch launch/patient launch/location launch/organization launch/encounter launch/resource)
ocpSmartPatientScope=(patient/Patient.read patient/Patient.write patient/RelatedPerson.read patient/RelatedPerson.write patient/EpisodeOfCare.read patient/EpisodeOfCare.write patient/Coverage.read patient/Coverage.write patient/Communication.read patient/Communication.write patient/Flag.read patient/Flag.write patient/ActivityDefinition.read patient/ActivityDefinition.write patient/Task.read patient/Task.write patient/Appointment.read patient/Appointment.write patient/AllergyIntolerence.read patient/AllergyIntolerence.write patient/Condition.read patient/Condition.write patient/Procedure.read patient/Procedure.write patient/ClinicalImpression.read patient/ClinicalImpression.write patient/Observation.read patient/Observation.write patient/Specimen.read patient/Specimen.write patient/DiagnosticReport.read patient/DiagnosticReport.write patient/Medication.read patient/Medication.write patient/MedicationRequest.read patient/MedicationRequest.write patient/MedicationDispense.read patient/MedicationDispense.write patient/MedicationStatement.read patient/MedicationStatement.write patient/CareTeam.read patient/CareTeam.write patient/CarePlan.read patient/CarePlan.write patient/Goal.read patient/Goal.write patient/ReferralRequest.read patient/ReferralRequest.write patient/ProcedureRequest.read patient/ProcedureRequest.write patient/AuditEvent.read patient/AuditEvent.write patient/Consent.read patient/Consent.write  patient/Provenance.read patient/Provenance.write)
ocpSmartUserScope=(user/Account.read user/Account.write user/ActivityDefinition.read user/ActivityDefinition.write user/AdverseEvent.read user/AdverseEvent.write user/AllergyIntolerance.read user/AllergyIntolerance.write user/Appointment.read user/Appointment.write user/AppointmentResponse.read user/AppointmentResponse.write user/AuditEvent.read user/AuditEvent.write user/Basic.read user/Basic.write user/Binary.read user/Binary.write user/BodySite.read user/BodySite.write user/Bundle.read user/Bundle.write user/CapabilityStatement.read user/CapabilityStatement.write user/CarePlan.read user/CarePlan.write user/CareTeam.read user/CareTeam.write user/ChargeItem.read user/ChargeItem.write user/Claim.read user/Claim.write user/ClaimResponse.read user/ClaimResponse.write user/ClinicalImpression.read user/ClinicalImpression.write user/CodeSystem.read user/CodeSystem.write user/Communication.read user/Communication.write user/CommunicationRequest.read user/CommunicationRequest.write user/CompartmentDefinition.read user/CompartmentDefinition.write user/Composition.read user/Composition.write user/ConceptMap.read user/ConceptMap.write user/Condition.read user/Condition.write user/Consent.read user/Consent.write user/Contract.read user/Contract.write user/Coverage.read user/Coverage.write user/DataElement.read user/DataElement.write user/DetectedIssue.read user/DetectedIssue.write user/Device.read user/Device.write user/DeviceComponent.read user/DeviceComponent.write user/DeviceMetric.read user/DeviceMetric.write user/DeviceRequest.read user/DeviceRequest.write user/DeviceUseStatement.read user/DeviceUseStatement.write user/DiagnosticReport.read user/DiagnosticReport.write user/DocumentManifest.read user/DocumentManifest.write user/DocumentReference.read user/DocumentReference.write user/EligibilityRequest.read user/EligibilityRequest.write user/EligibilityResponse.read user/EligibilityResponse.write user/Encounter.read user/Encounter.write user/Endpoint.read user/Endpoint.write user/EnrollmentRequest.read user/EnrollmentRequest.write user/EnrollmentResponse.read user/EnrollmentResponse.write user/EpisodeOfCare.read user/EpisodeOfCare.write user/ExpansionProfile.read user/ExpansionProfile.write user/ExplanationOfBenefit.read user/ExplanationOfBenefit.write user/FamilyMemberHistory.read user/FamilyMemberHistory.write user/Flag.read user/Flag.write user/Goal.read user/Goal.write user/GraphDefinition.read user/GraphDefinition.write user/Group.read user/Group.write user/GuidanceResponse.read user/GuidanceResponse.write user/HealthcareService.read user/HealthcareService.write user/ImagingManifest.read user/ImagingManifest.write user/ImagingStudy.read user/ImagingStudy.write user/Immunization.read user/Immunization.write user/ImmunizationRecommendation.read user/ImmunizationRecommendation.write user/ImplementationGuide.read user/ImplementationGuide.write user/Library.read user/Library.write user/Linkage.read user/Linkage.write user/List.read user/List.write user/Location.read user/Location.write user/Measure.read user/Measure.write user/MeasureReport.read user/MeasureReport.write user/Media.read user/Media.write user/Medication.read user/Medication.write user/MedicationAdministration.read user/MedicationAdministration.write user/MedicationDispense.read user/MedicationDispense.write user/MedicationRequest.read user/MedicationRequest.write user/MedicationStatement.read user/MedicationStatement.write user/MessageDefinition.read user/MessageDefinition.write user/MessageHeader.read user/MessageHeader.write user/NamingSystem.read user/NamingSystem.write user/NutritionOrder.read user/NutritionOrder.write user/Observation.read user/Observation.write user/OperationDefinition.read user/OperationDefinition.write user/OperationOutcome.read user/OperationOutcome.write user/Organization.read user/Organization.write user/Parameters.read user/Parameters.write user/Patient.read user/Patient.write user/PaymentNotice.read user/PaymentNotice.write user/PaymentReconciliation.read user/PaymentReconciliation.write user/Person.read user/Person.write user/PlanDefinition.read user/PlanDefinition.write user/Practitioner.read user/Practitioner.write user/PractitionerRole.read user/PractitionerRole.write user/Procedure.read user/Procedure.write user/ProcedureRequest.read user/ProcedureRequest.write user/ProcessRequest.read user/ProcessRequest.write user/ProcessResponse.read user/ProcessResponse.write user/Provenance.read user/Provenance.write user/Questionnaire.read user/Questionnaire.write user/QuestionnaireResponse.read user/QuestionnaireResponse.write user/ReferralRequest.read user/ReferralRequest.write user/RelatedPerson.read user/RelatedPerson.write user/RequestGroup.read user/RequestGroup.write user/ResearchStudy.read user/ResearchStudy.write user/ResearchSubject.read user/ResearchSubject.write user/RiskAssessment.read user/RiskAssessment.write user/Schedule.read user/Schedule.write user/SearchParameter.read user/SearchParameter.write user/Sequence.read user/Sequence.write user/ServiceDefinition.read user/ServiceDefinition.write user/Slot.read user/Slot.write user/Specimen.read user/Specimen.write user/StructureDefinition.read user/StructureDefinition.write user/StructureMap.read user/StructureMap.write user/Subscription.read user/Subscription.write user/Substance.read user/Substance.write user/SupplyDelivery.read user/SupplyDelivery.write user/SupplyRequest.read user/SupplyRequest.write user/Task.read user/Task.write user/TestReport.read user/TestReport.write user/TestScript.read user/TestScript.write user/ValueSet.read user/ValueSet.write user/VisionPrescription.read user/VisionPrescription.write)

assignSmartUserTo=(ocp.role.ocpAdmin ocp.role.patient ocp.role.careCoordinator ocp.role.careManager ocp.role.organizationAdministrator ocp.role.primaryCareProvider ocp.role.benefitsSpecialist ocp.role.healthAssistant ocp.role.frontOfficeReceptionist)

function showInformation()
{
echo -e "This script will setup ocp.role.smartUser and assign ocp.role.smartUser to one existing role"
}

function jsonValue() {
KEY=$1
awk -F"[{,:}]" '{for(i=1;i<=NF;i++){if($i~/'$KEY'/){print $(i+1)}}}'|tr -d '"'| sed -n 1p
}

#Get user Token
function getScimAdminToken(){
token=$(curl --silent\
        -H "Content-Type:application/x-www-form-urlencoded" \
		-H "Cache-Control: no-cache" \
        -X POST --data "client_id=$CLIENT_ID&grant_type=$GRANT_TYPE&client_secret=$CLIENT_SECTRET&username=$ADMIN_USERNAME&response_type=$RESPONSE_TYPE&password=$ADMIN_PASSWORD" "$UAA_BASE_URL/oauth/token" | jsonValue access_token)
echo -e $token

}

function getGroupId(){
tmp=${1//./%2E}
replacedString=${tmp//_/%5F}
result=$(curl --silent \
	-H "Authorization: $2" \
	-H "Cache-Control: no-cache" \
	GET "$UAA_BASE_URL/Groups?filter=displayName+eq+%22$replacedString%22")
groupId=`echo  -e $result | grep -Po '"id": *\K"[^"]*"'  | tr -d '"'`
echo -e $groupId
}

function createGroupMembership(){
response=$(curl --silent \
			-H "Authorization: $3" \
			-H "Cache-Control: no-cache" \
			-H "Content-Type: application/json" \
			POST --data '{"origin" : "uaa", "type" : "GROUP", "value" : "'"$2"'"}' "$UAA_BASE_URL/Groups/$1/members")
}

function createNewRole(){
roleId=$(curl --silent \
	-H "Cache-Control: no-cache" \
	-H "Content-Type: application/json" \
	-H "Authorization: $3" \
	POST --data '{"displayName": "$1", "description" : "$2"}' "$UAA_BASE_URL/Groups" | jsonValue id)
}

function createRoleAndAddScopes(){
tmp=${1//./%2E}
replacedString=${tmp//_/%5F}
#echo -e $replacedString
totalResults=$(curl --silent \
	-H "Authorization: $3" \
	-H "Cache-Control: no-cache" \
	GET "$UAA_BASE_URL/Groups?filter=displayName+eq+%22$replacedString%22" | jsonValue totalResults)

echo -e $(curl --silent \
	-H "Authorization: $3" \
	-H "Cache-Control: no-cache" \
	GET "$UAA_BASE_URL/Groups?filter=displayName+eq+%22$replacedString%22")

if [ "$totalResults" -eq 0 ]; then
	roleId=$(curl --silent \
		-H "Cache-Control: no-cache" \
		-H "Content-Type: application/json" \
		-H "Authorization: $3" \
		POST --data '{"displayName": "'"$1"'", "description" : "'"$2"'"}' "$UAA_BASE_URL/Groups"  | jsonValue id)

echo -e "Created the new role."

echo -e "Adding permissions to $1"
ocpSmartScope=( `echo ${ocpSmartLaunchScope[@]}` `echo ${ocpSmartPatientScope[@]}` `echo ${ocpSmartUserScope[@]}` )
echo "Number of permissions to be added: ${#ocpSmartScope[*]}"
for index in ${!ocpSmartScope[*]}
	do
		groupId=`getGroupId "${ocpSmartScope[$index]}" "${bearerToken}"`
#		echo -e $groupId
		createGroupMembership "${groupId}" "${roleId}" "${bearerToken}"
		echo -e "\n Done: $((index+1)) of ${#ocpSmartScope[*]}"
	done

else
   echo "Role already present. Exitting!"
fi
}

function assignSmartRoleToExistingRole(){
  smartUserId=`getGroupId "${SMART_ROLE_NAME}" "${bearerToken}"`
#  echo -e $smartUserId
for index in ${!assignSmartUserTo[*]}
	do
       existingRoleId=`getGroupId "${assignSmartUserTo[$index]}" "${bearerToken}"`
       #  echo -e $existingRoleId
       createGroupMembership "${smartUserId}" "${existingRoleId}" "${bearerToken}"
       echo -e "Assigned smartUser role to ${assignSmartUserTo[$index]}"
    done
}

clear
token=`getScimAdminToken`
bearerToken="bearer $token"
#echo $bearerToken
showInformation

echo -e "Step 1 of 2: Create role and assign smart scope"
echo -e "The smart role created will be: ${SMART_ROLE_NAME}"
createRoleAndAddScopes "${SMART_ROLE_NAME}" "${SMART_ROLE_DESCRIPTION}" "${bearerToken}"

echo -e "Step 2 of 2: Assign smartUser to existing ocp role"
assignSmartRoleToExistingRole

echo -e "DONE"

