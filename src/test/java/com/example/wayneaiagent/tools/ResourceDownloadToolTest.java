package com.example.wayneaiagent.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ResourceDownloadToolTest {

    @Test
    public void testDownloadResource() {
        ResourceDownloadTool tool = new ResourceDownloadTool();
        String url = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcREV2uD6i51hxPZ3O9-T8tmFP3ADnsNstE13VrUWTeShUhjuoLCbSRKbLGK&s";
        String fileName = "logo.png";
        String result = tool.downloadResource(url, fileName);
        assertNotNull(result);
    }
}
