package cz.neumimto;

import com.google.inject.Inject;
import cz.neumimto.configuration.ConfigMapper;

import cz.neumimto.ioc.IoC;
import cz.neumimto.utils.FileUtils;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.state.ServerAboutToStartEvent;
import org.spongepowered.api.plugin.Plugin;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.jar.JarFile;

/**
 * Created by NeumimTo on 29.4.2015.
 */
@Plugin(id = "NtRpg", name = "NtRpg",dependencies = "after:GUICreator")
public class NtRpgPlugin {
    public static String workingDir;
    public static JarFile pluginjar;
    private static String configPath = File.separator + "mods" + File.separator + "NtRpg";

    @Inject
    public Logger logger;

    public static GlobalScope GlobalScope;


    @Subscribe
    public void onPluginLoad(ServerAboutToStartEvent event) {
        long start = System.nanoTime();
        IoC ioc = IoC.get();

        ioc.logger = logger;
        Game game = event.getGame();
        ioc.registerInterfaceImplementation(Game.class, game);
        ioc.registerInterfaceImplementation(Logger.class, logger);
        ioc.registerDependency(this);

        try {
            workingDir = new File(".").getCanonicalPath() + configPath;
            pluginjar = FileUtils.getPluginJar();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Path path = Paths.get(workingDir);
        ConfigMapper.init("NtRpg",path);
        ioc.registerDependency(ConfigMapper.get("NtRpg"));
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        path = Paths.get(workingDir + File.separator + "database.properties");
        System.out.print("asd");
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("database.properties");
            try {
                Files.copy(resourceAsStream, path);
                logger.warn("File \"database.properties\" has been copied into a mods-folder/NtRpg, Configure it and start the server again.");
                game.getServer().shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Path p = Paths.get(workingDir + File.separator + "database.properties");
        FileUtils.createFileIfNotExists(p);

        Properties properties = new Properties();
        try (FileInputStream stream = new FileInputStream(p.toFile())) {
            properties.load(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Class.forName(properties.getProperty("hibernate.connection.driver_class"));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        logger.info("Creating EntityManager");
        EntityManager em = Persistence.createEntityManagerFactory("ntrpg", properties).createEntityManager();
        ioc.registerInterfaceImplementation(EntityManager.class, em);
        ioc.get(IoC.class, ioc);
        ResourceLoader rl = ioc.build(ResourceLoader.class);
        rl.loadJarFile(pluginjar, true);
        GlobalScope = ioc.build(GlobalScope.class);
        rl.loadExternalJars();
        ioc.postProcess();;

        double elapsedTime = (System.nanoTime() - start) / 1000000000.0;
        logger.info("NtRpg plugin successfully loaded in " + elapsedTime + " seconds");
    }
}
