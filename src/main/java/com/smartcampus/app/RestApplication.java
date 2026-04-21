package com.smartcampus.app;

import com.smartcampus.filter.LoggingFilter;
import com.smartcampus.mapper.GenericExceptionMapper;
import com.smartcampus.mapper.LinkedResourceNotFoundExceptionMapper;
import com.smartcampus.mapper.NotFoundExceptionMapper;
import com.smartcampus.mapper.RoomNotEmptyExceptionMapper;
import com.smartcampus.mapper.SensorUnavailableExceptionMapper;
import com.smartcampus.mapper.WebApplicationExceptionMapper;
import com.smartcampus.resource.DiscoveryResource;
import com.smartcampus.resource.SensorResource;
import com.smartcampus.resource.SensorRoom;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api/v1")
public class RestApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        // Resources
        classes.add(DiscoveryResource.class);
        classes.add(SensorRoom.class);
        classes.add(SensorResource.class);
        // Providers
        classes.add(LoggingFilter.class);
        classes.add(RoomNotEmptyExceptionMapper.class);
        classes.add(LinkedResourceNotFoundExceptionMapper.class);
        classes.add(SensorUnavailableExceptionMapper.class);
        classes.add(NotFoundExceptionMapper.class);
        classes.add(WebApplicationExceptionMapper.class);
        classes.add(GenericExceptionMapper.class);
        return classes;
    }
}
