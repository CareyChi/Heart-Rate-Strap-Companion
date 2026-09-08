package com.careychi.hrstrap;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class DisplayBackgroundPolicyTest {
    @Test public void classifiesLcdDescriptors() {
        assertEquals(DisplayBackgroundPolicy.PanelType.LCD,
                DisplayBackgroundPolicy.classifyPanelDescriptor("6.67 inch IPS LCD panel"));
        assertEquals(DisplayBackgroundPolicy.PanelType.LCD,
                DisplayBackgroundPolicy.classifyPanelDescriptor("TFT display panel"));
        assertEquals(DisplayBackgroundPolicy.PanelType.LCD,
                DisplayBackgroundPolicy.classifyPanelDescriptor("mini-led backlit LCD"));
    }

    @Test public void classifiesOledBeforeTftBackplaneWords() {
        assertEquals(DisplayBackgroundPolicy.PanelType.OLED,
                DisplayBackgroundPolicy.classifyPanelDescriptor("LTPO AMOLED panel"));
        assertEquals(DisplayBackgroundPolicy.PanelType.OLED,
                DisplayBackgroundPolicy.classifyPanelDescriptor("TFT OLED panel"));
        assertEquals(DisplayBackgroundPolicy.PanelType.OLED,
                DisplayBackgroundPolicy.classifyPanelDescriptor("P-OLED display"));
    }

    @Test public void leavesUnknownDescriptorsUnchanged() {
        assertEquals(DisplayBackgroundPolicy.PanelType.UNKNOWN,
                DisplayBackgroundPolicy.classifyPanelDescriptor("s6e3fc3 dsi cmd panel"));
        assertEquals(DisplayBackgroundPolicy.PanelType.UNKNOWN,
                DisplayBackgroundPolicy.classifyPanelDescriptor(""));
        assertEquals(DisplayBackgroundPolicy.PanelType.UNKNOWN,
                DisplayBackgroundPolicy.classifyPanelDescriptor(null));
    }
}
