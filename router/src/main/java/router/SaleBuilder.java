/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package router;

import domain.*;
import java.util.ArrayList;
import javax.swing.*;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.idempotent.*;
import org.apache.camel.util.toolbox.AggregationStrategies;

/**
 *
 * @author Ubaada
 */
public class SaleBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        
        //Get the sale update sent to the email adress by Vend
        from("imaps://outlook.office365.com?username=qurub623@student.otago.ac.nz"
                + "&password=" + getPassword("Your email account password")
                + "&searchTerm.subject=Vend:SaleUpdate"
                + "&debugMode=false" // set to true if you want to see the authentication details
                + "&folderName=INBOX") // change to whatever folder your Vend messages end up in
                .convertBodyTo(String.class)
                .log("${body}")
                .to("jms:queue:vend-new-sale");

        // POST the json update to sales webservice to register the sale
        from("jms:queue:vend-new-sale")
                .unmarshal().json(JsonLibrary.Gson, Sale.class)
                .marshal().json(JsonLibrary.Gson)
                .removeHeaders("*")
                .setHeader(Exchange.CONTENT_TYPE).constant("application/json")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .to("http://localhost:8081/api/sales")
                .log("New sale added")
                .to("jms:queue:sales-service-response");

        // Store the customer group to compare.
        // Get the summary of the customer's sale from the sales service
        // Extract the new calculated group to compare with old one.
        from("jms:queue:sales-service-response")
                // Store customer id and group
                .setProperty("customerId").jsonpath("$.customer.id")
                .setProperty("customerGroup").jsonpath("$.customer.customer_group_id")
                
                .removeHeaders("*") // remove headers to stop them being sent to the service
                .setBody(constant(null)) // can't pass a body in a GET request
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .toD("http://localhost:8081/api/sales/customer/${exchangeProperty.customerId}/summary")
                
                  // Calculated group_id from summary info
                .unmarshal().json(JsonLibrary.Gson, Summary.class)
                .setProperty("summaryGroup").simple("${body.getVendGroup}")
                .log("Total Sales : ${body.getNumberOfSales} , Total Payment : ${body.getTotalPayment} ")
                .log("Old Group : ${exchangeProperty.customerGroup}, "
                         + "Sum Group : ${exchangeProperty.summaryGroup} - ${body.getGroup} ")
                .to("jms:queue:sales-service-summary");
        
        // Compare both group code to check if updae is needed.
        from("jms:queue:sales-service-summary")
                .choice()
                .when().simple("${exchangeProperty.customerGroup} == ${exchangeProperty.summaryGroup}")
                    .log("Update not needed")
                    .to("jms:queue:end-queue")
                .otherwise()
                    .log("Customer upgraded")
                    .to("jms:queue:update-customer");
        
        // Update the customer/
        // Get customer from vend, update group field and then resend
        from("jms:queue:update-customer")
                // Get current state of the customer
                .removeHeaders("*") // remove headers to stop them being sent to the service
                .setBody(constant(null)) // can't pass a body in a GET request
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("authorization", constant("Bearer KiQSsELLtocyS2WDN5w5s_jYaBpXa0h2ex1mep1a"))
                .toD("https://info303otago.vendhq.com/api/2.0/customers/${exchangeProperty.customerId}")
                // Edit the group
                .setBody().jsonpath("$.data")
                .marshal().json(JsonLibrary.Gson)
                .unmarshal().json(JsonLibrary.Gson, Customer.class)
                .bean(Customer.class, "customerWithNewGroup(${exchangeProperty.summaryGroup})")
                // Send it back to vend
                .marshal().json(JsonLibrary.Gson)
                .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
                .setHeader("authorization", constant("Bearer KiQSsELLtocyS2WDN5w5s_jYaBpXa0h2ex1mep1a"))
                .toD("https://info303otago.vendhq.com/api/2.0/customers/${exchangeProperty.customerId}")
                // Send to next queue for updating accounts-service
                .setBody().jsonpath("$.data")
                .marshal().json(JsonLibrary.Gson)
                .unmarshal().json(JsonLibrary.Gson, Customer.class)
                .log("Vend Customer Updated")
                .to("jms:queue:update-account");
        
        // Update the account
        // Convert the customer object into an account obj and send it in a json format
        from("jms:queue:update-account")
                .bean(Account.class, "createAccountFromCustomer(${body})")
                .marshal().json(JsonLibrary.Gson)
                .removeHeaders("*")
                .setHeader(Exchange.CONTENT_TYPE).constant("application/json")
                .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
                .toD("http://localhost:8086/api/accounts/account/${exchangeProperty.customerId}")
                .log("Account Service Updated")
                .to("jms:queue:end-of-update");
                
                
                
    }

    // For generating password on run so there is no record of our password on src code.
    public static String getPassword(String prompt) {
        JPasswordField txtPasswd = new JPasswordField();
        int resp = JOptionPane.showConfirmDialog(null, txtPasswd, prompt,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (resp == JOptionPane.OK_OPTION) {
            String password = new String(txtPasswd.getPassword());
            return password;
        }
        return null;
    }
}
