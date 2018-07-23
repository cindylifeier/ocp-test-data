#!/bin/bash

UAA_BASE_URL="http://localhost:8080/uaa"
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

#Generate user data to create user
generate_post_create_user_data()
{
  cat <<EOF
      {
          "userName": "$username",
          "password": "$password",
           "name" : {
              "familyName" : "$lastName",
              "givenName" : "$firstName"
            },
           "emails" : [ {
               "value" : "$email",
               "primary" : true
         } ],
            "active" : true,
            "verified" : true,
            "origin" : ""
          }
EOF
}

function createUser(){
userId=$(curl --silent \
	-H "Cache-Control: no-cache" \
	-H "Content-Type: application/json" \
	-H "Authorization: $bearerToken" \
	POST --data "$(generate_post_create_user_data)" "$UAA_BASE_URL/Users" | jsonValue id)
}

#Generate user data to create userinfo
generate_post_create_userinfo()
{
  cat <<EOF
      {
          "user_id": ["$userId"],
          "resource": ["$resourceType"],
          "id" : ["$resourceId"],
		  "orgId" : ["$orgId"]
       }
EOF
}


createUserinfo(){
userinfo=$(curl --silent \
	-H "Cache-Control: no-cache" \
	-H "Content-Type: application/json" \
	-H "Authorization: $bearerToken" \
	POST --data "$(generate_post_create_userinfo)" "$UAA_BASE_URL/userinfo")
}


#Get role group id by display name
function getGroupId(){
tmp=${1//./%2E}
replacedString=${tmp//_/%5F}
result=$(curl --silent \
	-H "Authorization: $bearerToken" \
	-H "Cache-Control: no-cache" \
	GET "$UAA_BASE_URL/Groups?filter=displayName+eq+%22$replacedString%22")
groupId=`echo  -e $result | grep -Po '"id": *\K"[^"]*"'  | tr -d '"'`
}

# assign user as a member of role group
function createGroupMembership(){
response=$(curl --silent \
			-H "Authorization: $bearerToken" \
			-H "Cache-Control: no-cache" \
			-H "Content-Type: application/json" \
			POST --data '{"origin" : "uaa", "type" : "USER", "value" : "'"$userId"'"}' "http://localhost:8080/uaa/Groups/$groupId/members")
echo $response
}


clear
echo "This script is used to CREATE a user in UAA with fhir resource as userinfo"
echo -e "Please enter firstName : "
read firstName

echo -e "Please enter lastName : "
read lastName

echo -e "Please enter username : "
read username

echo -e "Please enter password : "
read password

echo -e "Please enter EMAIL : "
read email

echo -e "Please enter role scope : "
read scope

echo -e "Select fhir resource type from following: "
echo -e "  1 Practitioner"
echo -e "  2 Patient"
echo -e "  3 NONE"
echo -e "Enter fhir resource type: "
read resourceTypeChoice

if [ "$resourceTypeChoice" -eq 1 ]
then
  resourceType="Practitioner"
elif [ "$resourceTypeChoice" -eq 2 ]
then resourceType="Patient"
fi

if [ "$resourceTypeChoice" -eq 1 ] || [ "$resourceTypeChoice" -eq 2 ]
then
	echo -e "Please enter the Org ID to which this user belong to : "
	read orgId
	echo -e "Please choose fhir resource id: "
	read resourceId
fi

echo -e "${red}Step 1 of 4:  getting token...${reset}"
getScimAdminToken
bearerToken="bearer $token"
echo -e $bearerToken

echo -e "${red}Step 2 of 4: Creating user account...${reset}"
createUser
echo -e "Here is your userId: $userId"

echo -e "${red}Step 3 of 4: Creating userinfo with fhir resource...${reset}"
if [ "$resourceTypeChoice" -eq 1 ] || [ "$resourceTypeChoice" -eq 2 ]
then
createUserinfo
elif [ "$resourceTypeChoice" -eq 3 ]
then echo -e "Skip...  None fhir resource "
fi

echo -e "${red}Step 4 of 4: Assign role scope to user...${reset}"
getGroupId $scope

echo -e "Here is role scope group id: $groupId"
createGroupMembership

echo -e "DONE"
