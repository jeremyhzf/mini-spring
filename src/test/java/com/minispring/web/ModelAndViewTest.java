package com.minispring.web;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

public class ModelAndViewTest {

    @Test
    void shouldCreateModelAndView() {
        ModelAndView mav = new ModelAndView();
        assertNotNull(mav);
    }

    @Test
    void shouldSetAndGetView() {
        ModelAndView mav = new ModelAndView();
        mav.setView("testView");
        assertEquals("testView", mav.getView());
    }

    @Test
    void shouldAddAndGetModelData() {
        ModelAndView mav = new ModelAndView();
        mav.addObject("key", "value");
        assertEquals("value", mav.getModel().get("key"));
    }

    @Test
    void shouldCheckHasView() {
        ModelAndView mav = new ModelAndView();
        assertFalse(mav.hasView());
        mav.setView("test");
        assertTrue(mav.hasView());
    }
}
