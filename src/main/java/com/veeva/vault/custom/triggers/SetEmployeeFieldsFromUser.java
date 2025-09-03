/*
 * --------------------------------------------------------------------
 * RecordTrigger:	SetEmployeeFieldsFromUser
 * Object:			employee__c
 *---------------------------------------------------------------------
 * Description: If the Vault User is populated on an Employee record, default the First Name and Last Name
 * values on the Employee record from the User record.
 *---------------------------------------------------------------------
 * Copyright (c) 2022 Veeva Systems Inc.  All Rights Reserved.
 *---------------------------------------------------------------------
 */

package com.veeva.vault.custom.triggers;

import com.veeva.vault.custom.udc.EmployeeService;
import com.veeva.vault.custom.udc.EmployeeUserData;
import com.veeva.vault.sdk.api.core.*;
import com.veeva.vault.sdk.api.data.*;

import java.util.List;
import java.util.Map;

 @RecordTriggerInfo(object = "employee__c", events = {RecordEvent.BEFORE_INSERT, RecordEvent.BEFORE_UPDATE},
        order = TriggerOrder.NUMBER_2)
 public class SetEmployeeFieldsFromUser implements RecordTrigger {
    @Override
    public void execute(RecordTriggerContext recordTriggerContext) {

        //Instantiate the log service
        LogService logService = ServiceLocator.locate(LogService.class);

        //Determine if the record is insert (or update)
        boolean insert = recordTriggerContext.getRecordEvent().equals(RecordEvent.BEFORE_INSERT);

        // Iterate through the Employee records and add newly inserted employee records and
        // updated employee records with a Vault User change to a list
        List<String> updatedVaultUserIds = VaultCollections.newList();
        for (RecordChange inputRecord : recordTriggerContext.getRecordChanges()) {
            String vaultUserId = inputRecord.getNew().getValue("vault_user__c", ValueType.STRING);
            if (vaultUserId != null && (insert ||
                    !vaultUserId.equals(inputRecord.getOld().getValue("vault_user__c", ValueType.STRING)))) {
                updatedVaultUserIds.add(vaultUserId);
            }
        }

        //Continue with logic if any of the employee records have a new Vault User populated
        if (!updatedVaultUserIds.isEmpty()) {

            EmployeeService employeeService = ServiceLocator.locate(EmployeeService.class);
            Map<String, EmployeeUserData> userIdToNameMap = employeeService.getEmployeeUserDataMap(updatedVaultUserIds);

            //Set the first name and last name values from the map for each record that has a Vault User
            for (RecordChange inputRecord : recordTriggerContext.getRecordChanges()) {
                String userId = inputRecord.getNew().getValue("vault_user__c", ValueType.STRING);
                if (userId != null) {
                    if(userIdToNameMap.keySet().contains(userId)){
                        EmployeeUserData employeeUserData = userIdToNameMap.get(userId);
                        inputRecord.getNew().setValue("first_name__c", employeeUserData.getFirstName());
                        inputRecord.getNew().setValue("last_name__c", employeeUserData.getLastName());
                    }
                }
            }

            /*
            //Instantiate the QueryService
            QueryService queryService = ServiceLocator.locate(QueryService.class);
            //Retrieve the first and last names for the Vault Users
            Query query = queryService.newQueryBuilder()
                    .withSelect(VaultCollections.asList("id", "first_name__sys", "last_name__sys"))
                    .withFrom("user__sys")
                    .withWhere("id contains (${Custom.vault_user_ids})")
                    .build();

            //Use tokens to conserve resources instead of using string concatenation to add
            //user ids to the query string
            TokenService tokenService = ServiceLocator.locate(TokenService.class);
            TokenRequest userIdToken = tokenService.newTokenRequestBuilder()
                    .withValue("Custom.vault_user_ids", updatedVaultUserIds)
                    .build();

            //Create the query execution request with the query and token request
            QueryExecutionRequest queryExecutionRequest = queryService.newQueryExecutionRequestBuilder()
                    .withQuery(query)
                    .withTokenRequest(userIdToken)
                    .build();

            //Create a map to store list of user id mapping to first and last names returned by query
            Map<String, List<String>> userIdToNameMap = VaultCollections.newMap();

            //Execute the query
            queryService.query(queryExecutionRequest)
                    //If the query is successful, add id and name to map
                    .onSuccess(queryExecutionResponse -> {
                        queryExecutionResponse.streamResults().forEach(queryExecutionResult -> {
                            String userId = queryExecutionResult.getValue("id", ValueType.STRING);
                            String firstName = queryExecutionResult.getValue("first_name__sys", ValueType.STRING);
                            String lastName = queryExecutionResult.getValue("last_name__sys", ValueType.STRING);

                            //Create a String List of the User's First and Last Names
                            List<String> nameList = VaultCollections.newList();
                            nameList.add(firstName);
                            nameList.add(lastName);

                            userIdToNameMap.put(userId, nameList);
                        });
                    })
                    //If the query returns an error, log the error message and query string
                    .onError(queryOperationError -> {
                        if(logService.isErrorEnabled()) {
                            logService.error("Query error; error type = {}; query = {}",
                                    queryOperationError.getMessage(),
                                    queryOperationError.getQueryString());
                        }
                    })
                    .execute();

            //Set the first name and last name values from the map for each record that has a Vault User
            for (RecordChange inputRecord : recordTriggerContext.getRecordChanges()) {
                String userId = inputRecord.getNew().getValue("vault_user__c", ValueType.STRING);
                if (userId != null) {
                    if(userIdToNameMap.keySet().contains(userId)){
                        List<String> names = userIdToNameMap.get(userId);
                        inputRecord.getNew().setValue("first_name__c", names.get(0));
                        inputRecord.getNew().setValue("last_name__c", names.get(1));
                    }
                }
            }
             */
        }
    }
}