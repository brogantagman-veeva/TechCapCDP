/**
 * This trigger automatically creates a Shipment record
 * when a new Order record is created.
 */

package com.veeva.vault.custom.triggers;

import com.veeva.vault.sdk.api.core.LogService;
import com.veeva.vault.sdk.api.core.ServiceLocator;
import com.veeva.vault.sdk.api.core.*;
import com.veeva.vault.sdk.api.data.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.veeva.vault.sdk.api.data.Record;
import com.veeva.vault.sdk.api.document.DocumentService;
import com.veeva.vault.sdk.api.job.JobCompletionContext;
//import com.veeva.vault.sdk.api.job.JobInitiationContext;
import com.veeva.vault.sdk.api.job.JobLogger;
import com.veeva.vault.sdk.api.query.QueryService;
//import com.veeva.vault.sdk.api.workflow.WorkflowContext;


@RecordTriggerInfo(object = "order__c", events = {RecordEvent.AFTER_INSERT}, order = TriggerOrder.NUMBER_2)
public class OrderTrigger implements RecordTrigger {

    public void execute(RecordTriggerContext recordTriggerContext) {

        // Instantiate the log service
        LogService logService = ServiceLocator.locate(LogService.class);
        logService.info("OrderTrigger execution started.");

        //Make list of newly created order records
        List<Record> newOrders = VaultCollections.newList();

        for(RecordChange inputRecord : recordTriggerContext.getRecordChanges()) {
            newOrders.add(inputRecord.getNew());
        }

        //Proceed only if there are new orders to process
        if(!newOrders.isEmpty()) {
            // 1. Instantiate the RecordService, which is used to create, update, and delete records.
            RecordService recordService = ServiceLocator.locate(RecordService.class);
            List<Record> shipmentsToCreate = VaultCollections.newList();

            // 2. Loop through each new Order record that triggered this execution.
            for (Record order : newOrders) {

                // 3. For each Order, create a new in-memory instance of a 'shipment__c' record.
                Record newShipment = recordService.newRecord("shipment__c");

                // 4. Get necessary data from the parent Order record.
                String orderId = order.getValue("id", ValueType.STRING);
                LocalDate orderDate = order.getValue("order_date__c", ValueType.DATE);

                // 5. Set the fields on the new Shipment record.
                // Set the reference back to the originating order
                newShipment.setValue("order__c", orderId);

                // Calculate and set the expected shipment date (Order Date + 2 days)
                if (orderDate != null) {
                    LocalDate expectedShipmentDate = orderDate.plusDays(2);
                    newShipment.setValue("expected_shipment_date__c", expectedShipmentDate);
                }

                //Need to add system generated tracking number???

                // Add the fully prepared Shipment record to our list for creation.
                shipmentsToCreate.add(newShipment);
            }

            // 6. Save the list of new Shipment records to the database in a single transaction (bulkification).

            // Build the request without the unique field, as we are only inserting.
            RecordBatchSaveRequest request = recordService.newRecordBatchSaveRequestBuilder()
                    .withRecords(shipmentsToCreate)
                    .build();

            // Execute the save operation
            recordService.batchSaveRecords(request)
                    .onErrors(batchOperationErrors -> {
                        // This runs if any record fails to save. We'll grab the first error and stop everything.
                        batchOperationErrors.stream().findFirst().ifPresent(error -> {
                            String errMsg = error.getError().getMessage();
                            int errPosition = error.getInputPosition();

                            // Get the tracking number from the failing record to make the error message helpful.
                            String trackingNumber = shipmentsToCreate.get(errPosition).getValue("tracking_number__c", ValueType.STRING);

                            // Throwing a RollbackException stops the entire transaction.
                            throw new RollbackException("OPERATION_NOT_ALLOWED",
                                    "Unable to create Shipment with Tracking Number: " + trackingNumber + ". Reason: " + errMsg);
                        });
                    })
                    .onSuccesses(batchOperationSuccesses -> {
                        // This is a good place to log that the operation was successful.
                        logService.info("Successfully created {} new Shipment records.", batchOperationSuccesses.size());
                    })
                    .execute();
        }


        logService.info("OrderTrigger executed successfully.");

    }
}