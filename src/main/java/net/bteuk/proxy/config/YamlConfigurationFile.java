package net.bteuk.proxy.config;

import net.bteuk.proxy.Proxy;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

public class YamlConfigurationFile extends ConfigurationFile {

    private static final Yaml yaml;

    static {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yaml = new Yaml(options);
    }

    public YamlConfigurationFile(InputStream source, File destination) throws YAMLException, IOException {
        super(source, destination);
        FileInputStream input = null;

        try {
            input = new FileInputStream(file);
            values = yaml.load(input);
            if (values == null) {
                values = new LinkedHashMap<>();
            }
            input.close();
        } catch (YAMLException e) {
            if (input != null) {
                input.close();
            }
            throw e;
        }
    }

    @Override
    public void save() {
        try {
            Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            yaml.dump(values, writer);
            writer.close();
        } catch (IOException e) {
            Proxy.getInstance().getLogger().warn("Failed to save config.yml");
        }
    }
}
