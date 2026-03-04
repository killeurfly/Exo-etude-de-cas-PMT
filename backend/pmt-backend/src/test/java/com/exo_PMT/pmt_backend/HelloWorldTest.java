package com.exo_PMT.pmt_backend;

import com.exo_PMT.pmt_backend.controller.TestController;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class HelloWorldTest {

    @Test
    void testHelloWorld() {
        TestController controller = new TestController();
        String result = controller.hello();
        assertThat(result).isEqualTo("PMT backend is running!");
    }
}