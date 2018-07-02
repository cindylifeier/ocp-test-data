#!/bin/bash

UAA_BASE_URL="http://localhost:8080/uaa"

#admin_user
CLIENT_ID="admin-client"
CLIENT_SECTRET="changeit"
ADMIN_USERNAME="scim-admin"
ADMIN_PASSWORD="AAA!aaa1"
RESPONSE_TYPE="token"
GRANT_TYPE="password"

#smart client data
SMART_CLIENT_ID="cardiac_risk"
SMART_CLIENT_NAME="cardiac risk App"
SCOPE='"launch/patient", "patient/Observation.read"'
AUTHORITIES='"launch/patient", "patient/Observation.read"'
REDIRECT_URI="http://localhost:3000"
APP_LAUNCH_URL="http://localhost:3000/launch.html"

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
create_smart_client()
{
  cat <<EOF
     {
  "scope" : [$SCOPE],
  "client_id" : "$SMART_CLIENT_ID",
  "authorized_grant_types" : [ "authorization_code" ],
  "redirect_uri" : ["$REDIRECT_URI"],
  "autoapprove" : [ "false" ],
  "authorities" : [$AUTHORITIES],
  "allowedproviders" : [ "uaa" ],
  "name" : "$SMART_CLIENT_NAME",
  "checked": "true",
  "lastModified" : 1524009081219,
  "client_secret" : "changeit"
}
EOF
}

create_smart_client_meta(){
  cat <<EOF
    {
  "clientId" : "$SMART_CLIENT_ID",
  "showOnHomePage" : true,
  "appLaunchUrl" : "$APP_LAUNCH_URL"
  }
EOF
}

function createClient(){

echo -e $(create_smart_client)

client=$(curl --silent \
	-H "Cache-Control: no-cache" \
	-H "Content-Type: application/json" \
	-H "Authorization: $bearerToken" \
	POST --data "$(create_smart_client)" "$UAA_BASE_URL/oauth/clients")

echo -e $client
}

function createClientMeta(){

echo -e $(create_smart_client_meta)

clientMeta=$(curl -X PUT\
	-H "Content-Type: application/json" \
	-H "Authorization: $bearerToken" \
	-d "$(create_smart_client_meta)" "$UAA_BASE_URL/oauth/clients/$SMART_CLIENT_ID/meta")

echo -e $clientMeta
}



clear
echo "This script is used to register a smart client app"
echo "WARNNING: Please update your smart client info in the bash file"

echo -e "${red}Step 1 of 3:  getting token...${reset}"
getScimAdminToken
bearerToken="bearer $token"
echo -e $bearerToken

echo -e "${red}Step 2 of 3: Creating client with client id: $SMART_CLIENT_ID...${reset}"
createClient

echo -e "${red}Step 3 of 3: update client meta...${reset}"
createClientMeta

echo -e "DONE"
