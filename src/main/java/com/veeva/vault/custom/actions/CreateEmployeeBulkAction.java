/*
 * --------------------------------------------------------------------
 * RecordAction:	CreateEmployeeBulkAction
 * Object:			user__sys
 *---------------------------------------------------------------------
 * Description: User Action to bulk create Employee records from User records.
 *---------------------------------------------------------------------
 * Copyright (c) 2022 Veeva Systems Inc.  All Rights Reserved.
 *---------------------------------------------------------------------
 */

package com.veeva.vault.custom.actions;

import com.veeva.vault.sdk.api.action.RecordAction;
import com.veeva.vault.sdk.api.action.RecordActionContext;
import com.veeva.vault.sdk.api.action.RecordActionInfo;
import com.veeva.vault.sdk.api.action.Usage;
import com.veeva.vault.sdk.api.core.*;
import com.veeva.vault.sdk.api.data.PositionalRecordId;
import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.data.RecordBatchSaveRequest;
import com.veeva.vault.sdk.api.data.RecordService;

import java.util.List;

@RecordActionInfo(object="user__sys", label="SDK: Create Employee from User (bulk)", usages = Usage.USER_BULK_ACTION)
public class CreateEmployeeBulkAction implements RecordAction {
    @Override
    public void execute(RecordActionContext recordActionContext) {

        //Instantiate the log service
        LogService logService = ServiceLocator.locate(LogService.class);

        List<Record> userRecordList = recordActionContext.getRecords();
        List<Record> employeeRecordList = VaultCollections.newList();

        //Instantiate the record service
        RecordService recordService = ServiceLocator.locate(RecordService.class);

        //Create a new Employee record and set the Vault User value for each User record included in the context,
        //the SetEmployeeFieldsFromUser Record Trigger will automatically set the first name and last name values
        // which are used to set the name value on the record
        for (Record inputRecord : userRecordList) {
            Record employee_record = recordService.newRecord("employee__c");
            String userId = inputRecord.getValue("id", ValueType.STRING);
            employee_record.setValue("vault_user__c", userId);
            employeeRecordList.add(employee_record);
        }

        RecordBatchSaveRequest saveRequest = recordService.newRecordBatchSaveRequestBuilder()
                .withRecords(employeeRecordList)
                .build();

        //Batch save the record
        recordService.batchSaveRecords(saveRequest)
                .onSuccesses(positionalRecordIds -> {
                    if(logService.isInfoEnabled()) {
                        for(PositionalRecordId positionalRecordId : positionalRecordIds) {
                            logService.info("Employee record created for User ID: {}.",
                                    employeeRecordList.get(positionalRecordId.getInputPosition())
                                            .getValue("vault_user__c", ValueType.STRING));
                        }
                    }
                })
                .onErrors(batchOperationErrors -> {
                    // Iterate over the caught errors.
                    for (BatchOperationError error : batchOperationErrors) {
                        if(logService.isErrorEnabled()) {
                            String errMsg = error.getError().getMessage();
                            logService.error("Unable to save Employee record. Error: {}", errMsg);
                        }
                    }
                })
                .execute();
    }
    @Override
    public boolean isExecutable(RecordActionContext recordActionContext) {
        return true;
    }

}
