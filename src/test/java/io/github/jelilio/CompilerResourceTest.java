package io.github.jelilio;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class CompilerResourceTest {
    @Test
    void testHelloEndpoint() {
        given()
          .when().get("/compiler/")
          .then()
             .statusCode(200)
             .body(is("Compiler API"));
    }

}