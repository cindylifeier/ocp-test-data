#!/bin/bash

UAA_BASE_URL="http://172.16.112.35:8080/uaa"
CLIENT_ID="admin-client"
CLIENT_SECTRET="changeit"
ADMIN_USERNAME="scim-admin"
ADMIN_PASSWORD="AAA!aaa1"
RESPONSE_TYPE="token"
GRANT_TYPE="password"

red=`tput setaf 1`
reset=`tput sgr0`

#Get property value from json response
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
}

#Get role group id by display name
function getGroupId(){
#echo "\n Checking status..."
tmp=${1//./%2E}
replacedString=${tmp//_/%5F}
#echo -e "\n Replaced String : $replacedString"
result=$(curl --silent \
	-H "Authorization: $bearerToken" \
	-H "Cache-Control: no-cache" \
	GET "$UAA_BASE_URL/Groups?filter=displayName+eq+%22$replacedString%22")
groupId=`echo  -e $result | grep -Po '"id": *\K"[^"]*"'  | tr -d '"'`
}

#Get role group id by display name
function getRoleGroupId(){
#echo "\n Checking status..."
tmp=${1//./%2E}
replacedString=${tmp//_/%5F}
#echo -e "\n Replaced String : $replacedString"
result=$(curl --silent \
	-H "Authorization: $bearerToken" \
	-H "Cache-Control: no-cache" \
	GET "$UAA_BASE_URL/Groups?filter=displayName+eq+%22$replacedString%22")
roleId=`echo  -e $result | grep -Po '"id": *\K"[^"]*"'  | tr -d '"'`
}

# assign user as a member of role group
function createGroupMembership(){
response=$(curl --silent \
			-H "Authorization: $bearerToken" \
			-H "Cache-Control: no-cache" \
			-H "Content-Type: application/json" \
			POST --data '{"origin" : "uaa", "type" : "GROUP", "value" : "'"$roleId"'"}' "http://172.16.112.35:8080/uaa/Groups/$groupId/members")
echo $response
}


clear
echo -n "Please enter permission scope : "
read scope

echo -n "Please enter role scope : "
read role

echo -e "${red}Step 1 of 2:  getting token...${reset}"
getScimAdminToken
bearerToken="bearer $token"
echo -e $bearerToken

echo -e "${red}Step 2 of 2: Assign role scope to user...${reset}"
getGroupId $scope
echo -e "Here is permission scope group id: $groupId"

getRoleGroupId $role
echo -e "Here is role scope group id: $roleId"

createGroupMembership

echo -e "DONE"
