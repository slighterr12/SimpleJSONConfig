package com.twodevsstudio.simplejsonconfig.api;

import com.twodevsstudio.simplejsonconfig.def.Serializer;
import com.twodevsstudio.simplejsonconfig.exceptions.AnnotationProcessException;
import com.twodevsstudio.simplejsonconfig.interfaces.Autowired;
import com.twodevsstudio.simplejsonconfig.interfaces.Configuration;
import com.twodevsstudio.simplejsonconfig.utils.CustomLogger;
import dorkbox.annotation.AnnotationDefaults;
import dorkbox.annotation.AnnotationDetector;
import lombok.SneakyThrows;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

public class AnnotationProcessor {
    
    private Plugin instance;
    
    //private Reflections reflections;
    
    public static AnnotationProcessor INSTANCE;
    
    public static void processAnnotations(@NotNull Plugin plugin) {
        
        if (INSTANCE != null) throw new AnnotationProcessException();
        
        INSTANCE = new AnnotationProcessor(plugin);
        
        INSTANCE.processConfiguration();
        INSTANCE.processAutowired();
        
    }
    
    private AnnotationProcessor(@NotNull Plugin plugin) {
        this.instance = plugin;
        //this.reflections = new Reflections(instance.getClass().getPackage().getName());
    }
    
    @SneakyThrows
    private void processConfiguration() {
        
        //Set<Class<?>> configurationClasses = reflections.getTypesAnnotatedWith(Configuration.class);
        List<Class<?>> configurationClasses = AnnotationDetector.scanClassPath(instance.getClass().getPackage().getName()).forAnnotations(Configuration.class).collect(AnnotationDefaults.getType);
        
        for (Class<?> annotadedClass : configurationClasses) {
            
            Configuration configurationAnnotation = annotadedClass.getAnnotation(Configuration.class);
            String configName = configurationAnnotation.name();
            
            if (!isConfig(annotadedClass)) {
                CustomLogger.warning("Configuration " + configName + " could not be loaded. Class annotated as @Configuration does not extends " + Config.class.getName());
                continue;
            }
            
            Class<? extends Config> configClass = (Class<? extends Config>) annotadedClass;
            
            Constructor<? extends Config> constructor;
            Config config;
            
            try {
                constructor = configClass.getConstructor();
                constructor.setAccessible(true);
                config = constructor.newInstance();
            } catch (ReflectiveOperationException ignored) {
                CustomLogger.warning(configClass.getName() + ": Cannot find default constructor");
                continue;
            }
            
            String fileName = configName.endsWith(".json") ? configName : configName + ".json";
            
            initConfig(config, new File(instance.getDataFolder() + "/configuration", fileName));
            
        }
        
    }
    
    @SneakyThrows
    private void processAutowired() {
        List<Field> fields = AnnotationDetector.scanClassPath(instance.getClass().getPackage().getName()).forAnnotations(Autowired.class).on(ElementType.METHOD).collect(AnnotationDefaults.getField);
    
        for (Field field : fields) {
        
            field.setAccessible(true);
        
            Class<?> type = field.getType();
        
            if (type.getSuperclass() == Config.class && Modifier.isStatic(field.getModifiers())) {
                field.set(null, Config.getConfig((Class<? extends Config>) type));
            }
        
        }
        
    }
    
    public boolean isConfig(@NotNull Class<?> clazz) {
        return clazz.getSuperclass() == Config.class;
    }
    
    private void initConfig(@NotNull Config config, @NotNull File configFile) {
        
        if (!configFile.exists()) {
            
            try {
                configFile.createNewFile();
                Serializer.getInst().saveConfig(config, configFile);
            } catch (IOException ex) {
                ex.printStackTrace();
                return;
            }
            
        } else {
            
            try {
                config.reload();
            } catch (Exception exception) {
                CustomLogger.warning(config.getClass().getName() + ": Config file is corrupted");
                return;
            }
            
        }
        
        config.configFile = configFile;
        ConfigContainer.SINGLETONS.put(config.getClass(), config);
    }
    
}