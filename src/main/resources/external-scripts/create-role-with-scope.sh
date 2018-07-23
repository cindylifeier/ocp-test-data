#!/bin/bash
function showOptions()
{
echo -e "What role do you want to create today?"
echo -e "1. OCP Admin"
echo -e "2. Patient"
echo -e "3. Case Manager / Care Coordinator"
echo -e "4. Care Manager"
echo -e "5. Organization Admin"
echo -e "6. Primary Care Provider"
echo -e "7. Benefits Specialist"
echo -e "8. Health Assistant"
echo -e "9. Front Office Receptionist"

echo -e "Enter your choice :"
read choice
}

function jsonValue() {
KEY=$1
awk -F"[{,:}]" '{for(i=1;i<=NF;i++){if($i~/'$KEY'/){print $(i+1)}}}'|tr -d '"'| sed -n 1p
}

function getScimAdminToken(){
token=$(curl --silent\
        -H "Content-Type:application/x-www-form-urlencoded" \
		-H "Cache-Control: no-cache" \
        -X POST --data "client_id=admin-client&grant_type=password&client_secret=changeit&username=scim-admin&response_type=token&password=AAA%21aaa1" "http://localhost:8080/uaa/oauth/token" | jsonValue access_token)

echo -e $token
}

function getGroupId(){
tmp=${1//./%2E}
replacedString=${tmp//_/%5F}
result=$(curl --silent \
	-H "Authorization: $2" \
	-H "Cache-Control: no-cache" \
	GET "http://localhost:8080/uaa/Groups?filter=displayName+eq+%22$replacedString%22")
groupId=`echo  -e $result | grep -Po '"id": *\K"[^"]*"'  | tr -d '"'`
echo -e $groupId
}

function createGroupMembership(){
response=$(curl --silent \
			-H "Authorization: $3" \
			-H "Cache-Control: no-cache" \
			-H "Content-Type: application/json" \
			POST --data '{"origin" : "uaa", "type" : "GROUP", "value" : "'"$2"'"}' "http://localhost:8080/uaa/Groups/$1/members")
}

function createNewRole(){
roleId=$(curl --silent \
	-H "Cache-Control: no-cache" \
	-H "Content-Type: application/json" \
	-H "Authorization: $3" \
	POST --data '{"displayName": "$1", "description" : "$2"}' "http://localhost:8080/uaa/Groups" | jsonValue id)
}

function createRoleAndAddScopes(){
tmp=${1//./%2E}
replacedString=${tmp//_/%5F}
totalResults=$(curl --silent \
	-H "Authorization: $3" \
	-H "Cache-Control: no-cache" \
	GET "http://localhost:8080/uaa/Groups?filter=displayName+eq+%22$replacedString%22" | jsonValue totalResults)

if [ "$totalResults" = 0 ]; then
	roleId=$(curl --silent \
		-H "Cache-Control: no-cache" \
		-H "Content-Type: application/json" \
		-H "Authorization: $3" \
		POST --data '{"displayName": "'"$1"'", "description" : "'"$2"'"}' "http://localhost:8080/uaa/Groups"  | jsonValue id)

echo -e "Created the new role."

ocpAdminScope=(ocpUi.access ocpUiApi.appointment_update ocpUiApi.organization_create ocpUiApi.organization_read ocpUiApi.organization_update ocpUiApi.organization_delete ocpUiApi.patient_create ocpUiApi.patient_read ocpUiApi.patient_update ocpUiApi.patient_delete ocpUiApi.practitioner_create ocpUiApi.practitioner_read ocpUiApi.practitioner_update ocpUiApi.practitioner_delete ocpUiApi.location_create ocpUiApi.location_read ocpUiApi.location_update ocpUiApi.location_delete ocpUiApi.healthcareService_create ocpUiApi.healthcareService_read ocpUiApi.healthcareService_assign ocpUiApi.healthcareService_unassign ocpUiApi.healthcareService_update ocpUiApi.healthcareService_delete ocpUiApi.activityDefinition_create ocpUiApi.activityDefinition_read ocpUiApi.activityDefinition_update ocpUiApi.activityDefinition_delete ocpUiApi.relatedPerson_read ocpUiApi.group_read ocpUiApi.group_create ocpUiApi.group_update ocpUiApi.group_delete)

careCoordinatorScope=(ocpUi.access ocpUiApi.organization_read ocpUiApi.activityDefinition_read ocpUiApi.task_create ocpUiApi.task_read ocpUiApi.task_update ocpUiApi.task_delete ocpUiApi.patient_read ocpUiApi.patient_update ocpUiApi.careTeam_read ocpUiApi.appointment_create ocpUiApi.appointment_read ocpUiApi.appointment_update ocpUiApi.appointment_delete ocpUiApi.consent_create ocpUiApi.consent_read ocpUiApi.consent_update ocpUiApi.consent_delete ocpUiApi.communication_create ocpUiApi.communication_read ocpUiApi.communication_update ocpUiApi.communication_delete ocpUiApi.practitioner_read ocpUiApi.relatedPerson_read)

careManagerScope=(ocpUi.access ocpUiApi.organization_read ocpUiApi.activityDefinition_read ocpUiApi.task_create ocpUiApi.task_read ocpUiApi.task_update ocpUiApi.task_delete ocpUiApi.patient_create ocpUiApi.patient_read ocpUiApi.patient_update ocpUiApi.patient_delete ocpUiApi.careTeam_create ocpUiApi.careTeam_read ocpUiApi.careTeam_update ocpUiApi.careTeam_delete ocpUiApi.appointment_create ocpUiApi.appointment_read ocpUiApi.appointment_update ocpUiApi.appointment_delete ocpUiApi.consent_create ocpUiApi.consent_read ocpUiApi.consent_update ocpUiApi.consent_delete ocpUiApi.communication_create ocpUiApi.communication_read ocpUiApi.communication_update ocpUiApi.communication_delete ocpUiApi.practitioner_read ocpUiApi.relatedPerson_read)

orgAdminScope=(ocpUi.access ocpUiApi.location_create ocpUiApi.location_read ocpUiApi.location_update ocpUiApi.location_delete ocpUiApi.healthcareService_create ocpUiApi.healthcareService_read ocpUiApi.healthcareService_assign ocpUiApi.healthcareService_unassign ocpUiApi.healthcareService_update ocpUiApi.healthcareService_delete ocpUiApi.patient_create ocpUiApi.patient_read ocpUiApi.patient_update ocpUiApi.patient_delete ocpUiApi.practitioner_create ocpUiApi.practitioner_read ocpUiApi.practitioner_update ocpUiApi.practitioner_delete ocpUiApi.task_read ocpUiApi.appointment_read ocpUiApi.communication_read ocpUiApi.consent_read ocpUiApi.communication_read ocpUiApi.user_read ocpUiApi.user_create ocpUiApi.user_update ocpUiApi.user_delete ocpUiApi.group_read ocpUiApi.organization_read)

patientScope=(ocpUi.access ocpUiApi.organization_read ocpUiApi.activityDefinition_read ocpUiApi.appointment_read ocpUiApi.appointment_update ocpUiApi.careTeam_read ocpUiApi.careTeam_update ocpUiApi.careTeam_delete ocpUiApi.consent_create ocpUiApi.consent_read ocpUiApi.consent_update ocpUiApi.consent_delete ocpUiApi.relatedPerson_create ocpUiApi.relatedPerson_read ocpUiApi.relatedPerson_update ocpUiApi.relatedPerson_delete ocpUiApi.task_create ocpUiApi.task_read ocpUiApi.task_update ocpUiApi.task_delete)

pcpScope=(ocpUi.access ocpUiApi.organization_read  ocpUiApi.activityDefinition_read ocpUiApi.task_create ocpUiApi.task_read ocpUiApi.task_update ocpUiApi.task_delete ocpUiApi.patient_read ocpUiApi.careTeam_read ocpUiApi.appointment_read ocpUiApi.consent_create ocpUiApi.consent_read ocpUiApi.consent_update ocpUiApi.consent_delete ocpUiApi.communication_create ocpUiApi.communication_read ocpUiApi.communication_update ocpUiApi.communication_delete ocpUiApi.practitioner_read ocpUiApi.relatedPerson_read)

benefitsSpecialistScope=(ocpUi.access ocpUiApi.activityDefinition_read ocpUiApi.task_create ocpUiApi.task_read ocpUiApi.task_update ocpUiApi.task_delete ocpUiApi.patient_read ocpUiApi.careTeam_read ocpUiApi.appointment_create ocpUiApi.appointment_read ocpUiApi.appointment_update ocpUiApi.appointment_delete ocpUiApi.consent_create ocpUiApi.consent_read ocpUiApi.consent_update ocpUiApi.consent_delete ocpUiApi.communication_create ocpUiApi.communication_read ocpUiApi.communication_update ocpUiApi.communication_delete ocpUiApi.practitioner_read ocpUiApi.relatedPerson_read)

healthAssistantScope=(ocpUi.access ocpUiApi.activityDefinition_read ocpUiApi.task_read ocpUiApi.patient_read ocpUiApi.careTeam_read ocpUiApi.appointment_create ocpUiApi.appointment_read ocpUiApi.appointment_update ocpUiApi.appointment_delete ocpUiApi.consent_read ocpUiApi.communication_create ocpUiApi.communication_read ocpUiApi.communication_update ocpUiApi.communication_delete ocpUiApi.practitioner_read ocpUiApi.relatedPerson_read)

frontOfficeReceptionistScope=(ocpUi.access ocpUiApi.activityDefinition_read ocpUiApi.task_read ocpUiApi.patient_read ocpUiApi.appointment_create ocpUiApi.appointment_read ocpUiApi.appointment_update ocpUiApi.appointment_delete ocpUiApi.communication_create ocpUiApi.communication_read ocpUiApi.communication_update ocpUiApi.communication_delete ocpUiApi.practitioner_read ocpUiApi.relatedPerson_read)

case "$4" in
		1)
			echo -e "Adding permissions to $1"
			echo "Number of permissions to be added: ${#ocpAdminScope[*]}"
			for index in ${!ocpAdminScope[*]}
			do
				groupId=`getGroupId "${ocpAdminScope[$index]}" "${bearerToken}"`
				createGroupMembership "${groupId}" "${roleId}" "${bearerToken}"
				echo -e "\n Done: $((index+1)) of ${#ocpAdminScope[*]}"
			done
			break
			;;
		2)
			echo -e "Adding permissions to $1"
			echo "Number of permissions to be added: ${#patientScope[*]}"
			for index in ${!patientScope[*]}
			do
				groupId=`getGroupId "${patientScope[$index]}" "${bearerToken}"`
				createGroupMembership "${groupId}" "${roleId}" "${bearerToken}"
				echo -e "\n Done: $((index+1)) of ${#patientScope[*]}"
			done
			break
			;;
		3)
			echo -e "Adding permissions to $1"
			echo "Number of permissions to be added: ${#careCoordinatorScope[*]}"
			for index in ${!careCoordinatorScope[*]}
			do
				groupId=`getGroupId "${careCoordinatorScope[$index]}" "${bearerToken}"`
				createGroupMembership "${groupId}" "${roleId}" "${bearerToken}"
				echo -e "\n Done: $((index+1)) of ${#careCoordinatorScope[*]}"
			done
			break
			;;
		4)
			echo -e "Adding permissions to $1"
			echo "Number of permissions to be added: ${#careManagerScope[*]}"
			for index in ${!careManagerScope[*]}
			do
				groupId=`getGroupId "${careManagerScope[$index]}" "${bearerToken}"`
				createGroupMembership "${groupId}" "${roleId}" "${bearerToken}"
				echo -e "\n Done: $((index+1)) of ${#careManagerScope[*]}"
			done
			break
			;;
		5)
			echo -e "Adding permissions to $1"
			echo "Number of permissions to be added: ${#orgAdminScope[*]}"
			for index in ${!orgAdminScope[*]}
			do
				groupId=`getGroupId "${orgAdminScope[$index]}" "${bearerToken}"`
				createGroupMembership "${groupId}" "${roleId}" "${bearerToken}"
				echo -e "\n Done: $((index+1)) of ${#orgAdminScope[*]}"
			done
			break
			;;
		6)
			echo -e "Adding permissions to $1"
			echo "Number of permissions to be added: ${#pcpScope[*]}"
			for index in ${!pcpScope[*]}
			do
				groupId=`getGroupId "${pcpScope[$index]}" "${bearerToken}"`
				createGroupMembership "${groupId}" "${roleId}" "${bearerToken}"
				echo -e "\n Done: $((index+1)) of ${#pcpScope[*]}"
			done
			break
			;;
		7)
			echo -e "Adding permissions to $1"
			echo "Number of permissions to be added: ${#benefitsSpecialistScope[*]}"
			for index in ${!benefitsSpecialistScope[*]}
			do
				groupId=`getGroupId "${benefitsSpecialistScope[$index]}" "${bearerToken}"`
				createGroupMembership "${groupId}" "${roleId}" "${bearerToken}"
				echo -e "\n Done: $((index+1)) of ${#benefitsSpecialistScope[*]}"
			done
			break
			;;
		8)
			echo -e "Adding permissions to $1"
			echo "Number of permissions to be added: ${#healthAssistantScope[*]}"
			for index in ${!healthAssistantScope[*]}
			do
				groupId=`getGroupId "${healthAssistantScope[$index]}" "${bearerToken}"`
				createGroupMembership "${groupId}" "${roleId}" "${bearerToken}"
				echo -e "\n Done: $((index+1)) of ${#healthAssistantScope[*]}"
			done
			break
			;;
		9)
			echo -e "Adding permissions to $1"
			echo "Number of permissions to be added: ${#frontOfficeReceptionistScope[*]}"
			for index in ${!frontOfficeReceptionistScope[*]}
			do
				groupId=`getGroupId "${frontOfficeReceptionistScope[$index]}" "${bearerToken}"`
				createGroupMembership "${groupId}" "${roleId}" "${bearerToken}"
				echo -e "\n Done: $((index+1)) of ${#frontOfficeReceptionistScope[*]}"
			done
			break
			;;
		*)
			;;
		esac

else
   echo "Role already present. Exitting!"
fi
}

clear
token=`getScimAdminToken`
bearerToken="bearer $token"
#echo $bearerToken
echo -e "This script is used to create a role and add predefined scope to it"
showOptions

while [ choice != '' ]
    do
    if [[ $choice = "" ]]; then
            exit;
    else
		case "$choice" in
		1)
			echo -e "You have chosen to create a OCP Admin"
			role="ocp.role.ocpAdmin";
			description="OCP Admin Role";
			echo -e "The new role created will be: ${role}"
			createRoleAndAddScopes "${role}" "${description}" "${bearerToken}" "${choice}"
			break
			;;
		2)
			echo -e "You have chosen to create a Patient"
			role="ocp.role.patient";
			description="Patient Role";
			echo -e "The new role created will be: ${role}"
			createRoleAndAddScopes "${role}" "${description}" "${bearerToken}" "${choice}"
			break
			;;
		3)
			echo -e "You have chosen to create a Case Manager / Care Coordinator"
			role="ocp.role.careCoordinator";
			description="Case Manager/ Care Coordinator Role";
			echo -e "The new role created will be: ${role}"
			createRoleAndAddScopes "${role}" "${description}" "${bearerToken}" "${choice}"
			break
			;;
		4)
			echo -e "You have chosen to create a Care Manager"
			role="ocp.role.careManager";
			description="Care Manager Role";
			echo -e "The new role created will be: ${role}"
			createRoleAndAddScopes "${role}" "${description}" "${bearerToken}" "${choice}"
			break
			;;
		5)
			echo -e "You have chosen to create a Organization Admin"
			role="ocp.role.organizationAdministrator";
			description="Organization Administrator Role";
			echo -e "The new role created will be: ${role}"
			createRoleAndAddScopes "${role}" "${description}" "${bearerToken}" "${choice}"
			break
			;;
		6)
			echo -e "You have chosen to create a Primary Care Provider"
			role="ocp.role.primaryCareProvider";
			description="Primary Care Provider Role";
			echo -e "The new role created will be: ${role}"
			createRoleAndAddScopes "${role}" "${description}" "${bearerToken}" "${choice}"
			break
			;;
		7)
			echo -e "You have chosen to create a Benefits Specialist"
			role="ocp.role.benefitsSpecialist";
			description="Benefits Specialist Role";
			echo -e "The new role created will be: ${role}"
			createRoleAndAddScopes "${role}" "${description}" "${bearerToken}" "${choice}"
			break
			;;
		8)
			echo -e "You have chosen to create a Health Assistant"
			role="ocp.role.healthAssistant";
			description="Health Assistant Role";
			echo -e "The new role created will be: ${role}"
			createRoleAndAddScopes "${role}" "${description}" "${bearerToken}" "${choice}"
			break
			;;
		9)
			echo -e "You have chosen to create a Front Office Receptionist"
			role="ocp.role.frontOfficeReceptionist";
			description="Front Office Receptionist Role";
			echo -e "The new role created will be: ${role}"
			createRoleAndAddScopes "${role}" "${description}" "${bearerToken}" "${choice}"
			break
			;;
		x)
		    exit;
			;;

        \n)
            exit;
			;;
		*)
			echo -e "Unable to read your choice!"
			showOptions;
			;;
		esac
fi
done
