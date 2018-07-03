#!/bin/bash

UAA_BASE_URL="http://localhost:8080/uaa"
CLIENT_ID="admin-client"
CLIENT_SECTRET="changeit"
ADMIN_USERNAME="scim-admin"
ADMIN_PASSWORD="AAA!aaa1"
RESPONSE_TYPE="token"
GRANT_TYPE="password"

SMART_ROLE_NAME="ocp.role.smartAdmin"
SMART_ROLE_DESCRIPTION="ocp smart admin role"

ocpSmartAdminScope=(ocpSmart.client_read ocpSmart.client_create ocpSmart.client_update ocpSmart.client_delete)

assignSmartAdminTo=(ocp.role.ocpAdmin)

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
echo "Number of permissions to be added: ${#ocpSmartAdminScope[*]}"
for index in ${!ocpSmartAdminScope[*]}
	do
		groupId=`getGroupId "${ocpSmartAdminScope[$index]}" "${bearerToken}"`
#		echo -e $groupId
		createGroupMembership "${groupId}" "${roleId}" "${bearerToken}"
		echo -e "\n Done: $((index+1)) of ${#ocpSmartAdminScope[*]}"
	done

else
   echo "Role already present. Exitting!"
fi
}

function assignSmartRoleToExistingRole(){
  smartUserId=`getGroupId "${SMART_ROLE_NAME}" "${bearerToken}"`
#  echo -e $smartUserId
for index in ${!assignSmartAdminTo[*]}
	do
       existingRoleId=`getGroupId "${assignSmartAdminTo[$index]}" "${bearerToken}"`
       #  echo -e $existingRoleId
       createGroupMembership "${smartUserId}" "${existingRoleId}" "${bearerToken}"
       echo -e "Assigned smartUser role to ${assignSmartAdminTo[$index]}"
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

