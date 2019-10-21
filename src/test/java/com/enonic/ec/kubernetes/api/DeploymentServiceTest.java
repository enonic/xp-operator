//package com.enonic.ec.kubernetes.api;
//
//import org.junit.jupiter.api.Test;
//
//import io.quarkus.test.junit.QuarkusTest;
//
//import static io.restassured.RestAssured.given;
//import static org.hamcrest.CoreMatchers.is;
//
//@QuarkusTest
//class DeploymentServiceTest
//    extends ApiTest
//{
//
//    @Test
//    public void validInput()
//    {
//        given().
//            when().
//            get( "/api" ).
//            then().
//            statusCode( 200 ).
//            body( is( "hello" ) );
//    }
//}