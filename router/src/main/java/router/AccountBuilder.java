/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package router;

import domain.*;
import java.util.ArrayList;
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
public class AccountBuilder extends RouteBuilder {

    @Override
    public void configure() {  
        // End point for receiving form data from the account-ajax client.
        from("jetty:http://localhost:9000/api/account?enableCORS=true")
                // make message in-only so web browser doesn't have to wait on a non-existent response
                .setExchangePattern(ExchangePattern.InOnly)
                .log("${body}")
                .to("jms:queue:ajax-calls");

        // Convert the ajax call the account object.
        // Convert account object to Customer object
        // Convert customer object to json
        // POST the json  to Vend API
        // Store the response
        from("jms:queue:ajax-calls")
                .unmarshal().json(JsonLibrary.Gson, Account.class)
                .bean(Customer.class, "getCustomerFromAccount(${body})")
                .marshal().json(JsonLibrary.Gson)
                .removeHeader("*")
                .setHeader("authorization", constant("Bearer KiQSsELLtocyS2WDN5w5s_jYaBpXa0h2ex1mep1a"))
                .log("${body}")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .to("https://info303otago.vendhq.com/api/2.0/customers?bridgeEndpoint=true")
                .to("jms:queue:vend-new-cutomer-response");


        // Convert the vend response to Customer Object.
        // Convert the customer objectt to account object.
        // Marshall the account obj to json.
        // Store in a queue
        from("jms:queue:vend-new-cutomer-response")
                .setBody().jsonpath("$.data")
                .marshal().json(JsonLibrary.Gson)
                .unmarshal().json(JsonLibrary.Gson, Customer.class)
                .bean(Account.class, "createAccountFromCustomer(${body})")
                .marshal().json(JsonLibrary.Gson)
                .to("jms:queue:account-send");
    
        // POST the json account obj to accounts service
        from("jms:queue:account-send")
                .removeHeaders("*")
                .setHeader(Exchange.CONTENT_TYPE).constant("application/json")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .to("http://localhost:8086/api/accounts")
                .to("jms:queue:account-service-response");
    }

}
