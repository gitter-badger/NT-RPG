package cz.neumimto;

import cz.neumimto.commands.CommandBase;
import cz.neumimto.commands.CommandService;
import cz.neumimto.configuration.ConfigMapper;
import cz.neumimto.configuration.ConfigurationContainer;
import cz.neumimto.configuration.PluginConfig;
import cz.neumimto.effects.EffectService;
import cz.neumimto.effects.IGlobalEffect;
import cz.neumimto.ioc.*;
import cz.neumimto.players.properties.PlayerPropertyService;
import cz.neumimto.players.properties.Property;
import cz.neumimto.players.properties.PropertyContainer;
import cz.neumimto.skills.ISkill;
import cz.neumimto.skills.SkillService;
import org.slf4j.Logger;
import org.spongepowered.api.Game;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by NeumimTo on 27.12.2014.
 */
@Singleton
public class ResourceLoader {
    private final static String INNERCLASS_SEPARATOR = "$";

    //TODO use nio instead of io
    public static File classDir, raceDir, guildsDir, addonDir, skilltreeDir;
    private static IoC ioc;

    @Inject
    private LoggingService loggingService;

    @Inject
    private SkillService skillService;

    @Inject
    private GroupService groupService;

    @Inject
    private EffectService effectService;

    @Inject
    private PlayerPropertyService playerPropertyService;

    @Inject
    private Logger logger;

    @Inject
    private CommandService commandService;

    @Inject
    private ConfigMapper configMapper;

    static {
        classDir = new File(NtRpgPlugin.workingDir + File.separator + "classes");
        raceDir = new File(NtRpgPlugin.workingDir + File.separator + "races");
        guildsDir = new File(NtRpgPlugin.workingDir + File.separator + "guilds");
        addonDir = new File(NtRpgPlugin.workingDir + File.separator + "addons");
        skilltreeDir = new File(NtRpgPlugin.workingDir + File.separator + "skilltrees");
        classDir.mkdirs();
        raceDir.mkdirs();
        guildsDir.mkdirs();
        skilltreeDir.mkdirs();
        addonDir.mkdirs();
        ioc = IoC.get();
    }

    public ResourceLoader() {

    }

    public void loadExternalJars() {
        Path dir = addonDir.toPath();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.{jar|zip}")) {
            for (Path entry : stream) {
                loadJarFile(new JarFile(entry.toFile()), false);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadJarFile(JarFile file, boolean main) {
        if (file == null)
            return;
        logger.info("Loading jarfile " + file.getName());
        Enumeration<JarEntry> entries = file.entries();
        JarEntry next = null;
        ResourceClassLoader cl = new ResourceClassLoader((URLClassLoader) this.getClass().getClassLoader());
        while (entries.hasMoreElements()) {
            next = entries.nextElement();
            if (next.isDirectory() || !next.getName().endsWith(".class")) {
                continue;
            }
            if (main && !next.getName().startsWith("src/main/java/neumimto"))
                continue;
            if (next.getName().lastIndexOf(INNERCLASS_SEPARATOR) > 1)
                continue;
            String className = next.getName().substring(0, next.getName().length() - 6);
            className = className.replace('/', '.');
            Class<?> clazz = null;
            try {
                clazz = cl.loadClass(className);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                continue;
            }
            loadClass(clazz);
        }
        logger.info("Finished loading of jarfile " + file.getName());
    }


    public void loadClass(Class<?> clazz) {
        if (clazz.isInterface())
            return;
        if (Modifier.isAbstract(clazz.getModifiers())) {
            return;
        }
        if (clazz.isEnum())
            return;
        if (PluginConfig.DEBUG)
            logger.debug(" - Checking if theres something to load in a class " + clazz.getName());
        //Properties
        Object container = null;
        if (clazz.isAnnotationPresent(Singleton.class)) {
            container = ioc.build(clazz);
        }
        if (clazz.isAnnotationPresent(Listener.class)) {
            logger.debug("Registering listener" + clazz.getName());
            container = ioc.build(clazz);
            ioc.build(Game.class).getEventManager().register(ioc.build(NtRpgPlugin.class), container);
        }
        if (clazz.isAnnotationPresent(Command.class)) {
            container = ioc.build(clazz);
            logger.debug("registering command class" + clazz.getName());
            commandService.registerCommand((CommandBase) container);
        }
        if (clazz.isAnnotationPresent(Skill.class)) {
            container = ioc.build(clazz);
            logger.debug("registering skill " + clazz.getName());
            skillService.addSkill((ISkill) container);
        }
        if (clazz.isAnnotationPresent(ConfigurationContainer.class)) {
            configMapper.loadClass(clazz);
        }
        if (clazz.isAnnotationPresent(PropertyContainer.class)) {
            if (container == null)
                container = newInstance(clazz, clazz);
            for (Field f : container.getClass().getDeclaredFields()) {
                if (f.isAnnotationPresent(Property.class)) {
                    Property p = f.getAnnotation(Property.class);
                    try {
                        f.setShort(null, PlayerPropertyService.LAST_ID);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (!p.name().trim().equalsIgnoreCase("")) {
                        try {
                            playerPropertyService.registerProperty(p.name(), f.getShort(null));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                            continue;
                        }
                    }
                    if (p.default_() != 0f) {
                        playerPropertyService.registerDefaultValue(playerPropertyService.LAST_ID, p.default_());
                    }
                    playerPropertyService.LAST_ID = (short) (playerPropertyService.LAST_ID + 1);
                }
            }

        }
        //Effects
        if (IGlobalEffect.class.isAssignableFrom(clazz)) {
            IGlobalEffect i = newInstance(IGlobalEffect.class, clazz);
            effectService.registerGlobalEffect(i);
        }

    }

    private static <T> T newInstance(Class<T> excepted, Class<?> clazz) {
        T t = null;
        try {
            t = (T) clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return t;
    }


}
