package cz.neumimto.persistance;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import cz.neumimto.ResourceLoader;
import cz.neumimto.ioc.Inject;
import cz.neumimto.ioc.Singleton;
import cz.neumimto.skills.*;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by NeumimTo on 24.7.2015.
 */
@Singleton
public class SkillTreeDao {

    @Inject
    SkillService skillService;

    public Map<String, ? extends SkillTree> getAll() {
        Path dir = ResourceLoader.skilltreeDir.toPath();
        Map<String, SkillTree> map = new HashMap<>();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(dir, "*.conf")) {
            paths.forEach(path -> {
                Config config = ConfigFactory.parseFile(path.toFile());
                SkillTree skillTree = new SkillTree();
                skillTree.setDescription(config.getString("Description"));
                skillTree.setId(config.getString("Name"));
                Config sub = config.getObject("Skills").toConfig();
                for (Map.Entry<String, ConfigValue> entry : sub.root().entrySet()) {
                    SkillInfo info = getSkillInfo(entry.getKey(), skillTree);
                    ISkill skill = skillService.getSkill(info.getSkillName());

                    ConfigObject value = (ConfigObject) entry.getValue();
                    Config c = value.toConfig();
                    info.setMinPlayerLevel(c.getInt("MinPlayerLevel"));
                    info.setMinPlayerLevel(c.getInt("MaxSkillLevel"));
                    for (String conflicts : c.getStringList("Conflicts")) {
                        info.getConflicts().add(getSkillInfo(conflicts, skillTree));
                    }
                    for (String conflicts : c.getStringList("SoftDepends")) {
                        SkillInfo i = getSkillInfo(conflicts, skillTree);
                        info.getSoftDepends().add(i);
                        i.getDepending().add(info);
                    }
                    for (String conflicts : c.getStringList("HardDepends")) {
                        SkillInfo i = getSkillInfo(conflicts, skillTree);
                        info.getHardDepends().add(i);
                        i.getDepending().add(info);
                    }
                    Config settings = c.getConfig("SkillSettings");
                    SkillSettings skillSettings = new SkillSettings();
                    for (Map.Entry<String, ConfigValue> e : settings.entrySet()) {
                        if (e.getKey().endsWith(SkillSettings.bonus)) {
                            continue;
                        }
                        String val = e.getValue().render();
                        //dont create a new instances of string keys when we can use reference
                        if (skill.getDefaultSkillSettings().hasNode(e.getKey())) {
                            float norm = Float.parseFloat(val);
                            Map.Entry<String, Float> q = skill.getDefaultSkillSettings().getFloatNodeEntry(e.getKey());
                            Map.Entry<String, Float> w = skill.getDefaultSkillSettings().getFloatNodeEntry(e.getKey() + SkillSettings.bonus);
                            skillSettings.addNode(q.getKey(),norm);
                            float bon = Float.parseFloat(settings.getString(w.getKey()));
                            skillSettings.addNode(w.getKey(),bon);
                        }
                    }
                    info.setSkillSettings(skillSettings);
                    skillTree.getSkills().put(info.getSkillName(), info);
                    map.put(info.getSkillName().toLowerCase(), skillTree);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    private SkillInfo getSkillInfo(String name, SkillTree tree) {
        SkillInfo info = tree.getSkills().get(name);
        if (info == null) {
            info = new SkillInfo(name);
            tree.getSkills().put(name, info);
        }
        return info;
    }
}
