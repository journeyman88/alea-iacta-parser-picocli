/*
 * Copyright 2021 Marco Bignami.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.unknowndomain.alea.parser;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import net.unknowndomain.alea.messages.MsgBuilder;
import net.unknowndomain.alea.messages.MsgStyle;
import net.unknowndomain.alea.messages.ReturnMsg;
import net.unknowndomain.alea.systems.RpgSystemOptions;
import net.unknowndomain.alea.systems.annotations.RpgSystemData;
import net.unknowndomain.alea.systems.annotations.RpgSystemOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.TypeConversionException;
import picocli.CommandLine.UnmatchedArgumentException;

/**
 *
 * @author journeyman
 */
public class PicocliParser
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PicocliParser.class);
    
    public static void parseArgs(RpgSystemOptions options, String ... args)
    {
        parseArgs(options, Locale.ENGLISH, args);
    }
    
    public static void parseArgs(RpgSystemOptions options, Locale lang, String ... args)
    {
        CommandSpec spec = buildOptions(options.getClass(), lang);
        CommandLine cmd = new CommandLine(spec);
        try 
        {
            ParseResult result = cmd.parseArgs(args);
            parseOptions(options, result);
        }
        catch (TypeConversionException ex)
        {
            options.setHelp(true);
        }
        catch (Exception ex)
        {
            LOGGER.error(options.getClass().getCanonicalName(), ex);
            options.setHelp(true);
        }
    
    }
    public static ReturnMsg printHelp(String commandName, RpgSystemOptions options)
    {
        return printHelp(commandName, options, Locale.ENGLISH);
    }
    
    public static ReturnMsg printHelp(String commandName, RpgSystemOptions options, Locale lang)
    {
        MsgBuilder mb = new MsgBuilder();
        StringWriter out = new StringWriter();
        PrintWriter pw = new PrintWriter(out);
        CommandSpec spec = buildOptions(options.getClass(), lang);
        CommandLine cmd = new CommandLine(spec);
        cmd.setCommandName(commandName);
        cmd.usage(pw);
        pw.flush();
        mb.append(out.toString(), MsgStyle.CODE);
        return mb.build();
    }
    
    private static CommandSpec buildOptions(Class<? extends RpgSystemOptions> optionsClass, Locale lang)
    {
        CommandSpec spec = CommandSpec.create(); 
        List<Field> fields = new ArrayList<>();
        Class workingClass = optionsClass;
        while (true)
        {
            fields.addAll(Arrays.asList(workingClass.getDeclaredFields()));
            if (Objects.equals(RpgSystemOptions.class, workingClass))
            {
                break;
            }
            workingClass = workingClass.getSuperclass();
        }
        if (optionsClass.isAnnotationPresent(RpgSystemData.class)) 
        {
            RpgSystemData data = optionsClass.getAnnotation(RpgSystemData.class);
            spec.resourceBundle(ResourceBundle.getBundle(data.bundleName(), lang));
        }
        else
        {
            spec.resourceBundle(ResourceBundle.getBundle("net.unknowndomain.alea.systems.RpgSystemBundle", lang));
        }
        for (Field field : fields) 
        {
            field.setAccessible(true);
            if (field.isAnnotationPresent(RpgSystemOption.class)) {
                RpgSystemOption[] annotations = field.getAnnotationsByType(RpgSystemOption.class);

                for(RpgSystemOption annotation : annotations){
                    
                    List<String> optName = new ArrayList<>();
                    
                    if (!annotation.shortcode().isEmpty())
                    {
                        optName.add("-" + annotation.shortcode());
                    }
                    if (!annotation.name().isEmpty())
                    {
                        optName.add("--" + annotation.name());
                    }
                    
                    if (!optName.isEmpty())
                    {
                        String [] names = new String [optName.size()];
                        optName.toArray(names);
                        OptionSpec optSpec = OptionSpec.builder(names)
                                .descriptionKey(annotation.description())
                                .paramLabel(annotation.argName())
                                .type(field.getType())
                                .build();
                        spec.addOption(optSpec);
                    }
                }
            }
        }
        spec.usageMessage().sortOptions(false);
        return spec;
    }

    private static void parseOptions(RpgSystemOptions options, ParseResult result)
    {
        List<Field> fields = new ArrayList<>();
        Class workingClass = options.getClass();
        while (true)
        {
            fields.addAll(Arrays.asList(workingClass.getDeclaredFields()));
            if (Objects.equals(RpgSystemOptions.class, workingClass))
            {
                break;
            }
            workingClass = workingClass.getSuperclass();
        }
        for (Field field : fields) 
        {
            field.setAccessible(true);
            if (field.isAnnotationPresent(RpgSystemOption.class)) {
                RpgSystemOption[] annotations = field.getAnnotationsByType(RpgSystemOption.class);

                for(RpgSystemOption annotation : annotations){
                    
                    String optName = null;
                    if (!annotation.name().isEmpty())
                    {
                        optName = "--" + annotation.name();
                    }
                    if (!annotation.shortcode().isEmpty())
                    {
                        optName = "-" + annotation.shortcode();
                    }
                    
                    if ((optName != null) && (result.hasMatchedOption(optName)))
                    {
                        try
                        {
                            field.set(options, result.matchedOption(optName).getValue());
                        } catch (IllegalArgumentException | IllegalAccessException ex)
                        {
                            LOGGER.error(null, ex);
                        }
                    }
                }
            }
        }
    }
}
