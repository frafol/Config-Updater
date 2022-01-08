package com.tchristofferson.configupdater;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.*;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigUpdaterTest {

    private static final String FILE_NAME = "config.yml";
    private static final List<String> ignoredSections = Collections.singletonList("key6-ignored");
    private static Plugin plugin;

    @BeforeClass
    public static void beforeClass() {
        plugin = mock(Plugin.class);
        when(plugin.getResource(anyString())).then((Answer<InputStream>) invocationOnMock -> ConfigUpdaterTest.class.getClassLoader().getResourceAsStream(FILE_NAME));
    }

    @AfterClass
    public static void afterClass() {
        new File(FILE_NAME).delete();
    }

    @Before
    public void before() throws IOException, URISyntaxException {
        saveDefaultConfig(new File(FILE_NAME));
    }

    @Test
    public void testUpdateMethodToCheckIfFilesAreSameAfter() throws IOException, URISyntaxException {
        File toUpdate = new File(FILE_NAME);

        //config.yml uses \r\n for new lines whereas after update uses \n
        String preUpdateContent = new String(Files.readAllBytes(getResourcePath())).replace("\r\n", "\n");
        ConfigUpdater.update(plugin, FILE_NAME, toUpdate, ignoredSections);
        String postUpdateContent = new String(Files.readAllBytes(toUpdate.toPath())).trim();

        assertEquals(preUpdateContent, postUpdateContent);
    }

    @Test
    public void testUpdateMethodToMakeSureIgnoredSectionsAreHandledCorrectly() throws IOException, InvalidConfigurationException {
        File toUpdate = new File(FILE_NAME);

        FileConfiguration config = YamlConfiguration.loadConfiguration(toUpdate);
        config.set("a-section-with-ignored-sections.sub-ignored.ignored.value3", 3);
        config.set("a-section-with-ignored-sections.sub-ignored.ignored2.value", 1);

        config.save(toUpdate);
        ConfigUpdater.update(plugin, FILE_NAME, toUpdate, "a-section-with-ignored-sections.sub-ignored");
        config.load(toUpdate);

        assertTrue(config.contains("a-section-with-ignored-sections.sub-ignored.ignored.value3"));
        assertTrue(config.contains("a-section-with-ignored-sections.sub-ignored.ignored2.value"));
        assertEquals(config.getInt("a-section-with-ignored-sections.sub-ignored.ignored.value3"), 3);
        assertEquals(config.getInt("a-section-with-ignored-sections.sub-ignored.ignored2.value"), 1);
    }

    private void saveDefaultConfig(File toUpdate) throws IOException, URISyntaxException {
        Path path = getResourcePath();
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(Files.newBufferedReader(path));
        configuration.save(toUpdate);
    }

    private Path getResourcePath() throws URISyntaxException {
        URL preUpdateUrl = getClass().getClassLoader().getResource(FILE_NAME);
        return Paths.get(preUpdateUrl.toURI());
    }
}