package com.opensymphony.xwork2.conversion.impl;

import com.opensymphony.xwork2.conversion.ConversionAnnotationProcessor;
import com.opensymphony.xwork2.conversion.TypeConverter;
import com.opensymphony.xwork2.conversion.TypeConverterCreator;
import com.opensymphony.xwork2.conversion.TypeConverterHolder;
import com.opensymphony.xwork2.conversion.annotations.ConversionRule;
import com.opensymphony.xwork2.conversion.annotations.ConversionType;
import com.opensymphony.xwork2.conversion.annotations.TypeConversion;
import com.opensymphony.xwork2.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Default implementation of {@link ConversionAnnotationProcessor}
 */
public class DefaultConversionAnnotationProcessor implements ConversionAnnotationProcessor {

    private static final Logger LOG = LogManager.getLogger(DefaultConversionAnnotationProcessor.class);

    private TypeConverterCreator converterCreator;
    private TypeConverterHolder converterHolder;

    @Inject
    public void setTypeConverterCreator(TypeConverterCreator converterCreator) {
        this.converterCreator = converterCreator;
    }

    @Inject
    public void setTypeConverterHolder(TypeConverterHolder converterHolder) {
        this.converterHolder = converterHolder;
    }

    public void process(Map<String, Object> mapping, TypeConversion tc, String key) {
        LOG.debug("TypeConversion [{}/{}] with key: [{}]", tc.converter(), tc.converterClass(), key);
        if (key == null) {
            return;
        }
        try {
            if (tc.type() == ConversionType.APPLICATION) {
                if (StringUtils.isNoneEmpty(tc.converter())) {
                    converterHolder.addDefaultMapping(key, converterCreator.createTypeConverter(tc.converter()));
                } else {
                    converterHolder.addDefaultMapping(key, converterCreator.createTypeConverter(tc.converterClass()));
                }
            } else {
                if (tc.rule() == ConversionRule.KEY_PROPERTY || tc.rule() == ConversionRule.CREATE_IF_NULL) {
                    mapping.put(key, tc.value());
                }
                //for properties of classes
                else if (tc.rule() != ConversionRule.ELEMENT || tc.rule() == ConversionRule.KEY || tc.rule() == ConversionRule.COLLECTION) {
                    if (StringUtils.isNoneEmpty(tc.converter())) {
                        mapping.put(key, converterCreator.createTypeConverter(tc.converter()));
                    } else {
                        mapping.put(key, converterCreator.createTypeConverter(tc.converterClass()));
                    }
                }
                //for keys of Maps
                else if (tc.rule() == ConversionRule.KEY) {
                    Class<?> converterClass;
                    if (StringUtils.isNoneEmpty(tc.converter())) {
                        converterClass = Thread.currentThread().getContextClassLoader().loadClass(tc.converter());
                        //check if the converter is a type converter if it is one
                        //then just put it in the map as is. Otherwise
                        //put a value in for the type converter of the class
                    } else {
                        converterClass = tc.converterClass();
                    }

                    LOG.debug("Converter class: [{}]", converterClass);

                    if (converterClass.isAssignableFrom(TypeConverter.class)) {
                        mapping.put(key, converterCreator.createTypeConverter(tc.converter()));
                    } else {
                        mapping.put(key, converterClass);
                        LOG.debug("Object placed in mapping for key [{}] is [{}]", key, mapping.get(key));
                    }
                }
                //elements(values) of maps / lists
                else {
                    if (StringUtils.isNoneEmpty(tc.converter())) {
                        mapping.put(key, Thread.currentThread().getContextClassLoader().loadClass(tc.converter()));
                    } else {
                        mapping.put(key, tc.converterClass());
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Got exception for {}", key, e);
        }
    }

}
