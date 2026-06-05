package com.bhagwat.scm.core.rest.util;

import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;

public class PathFilterUtil {
    private static final PathPatternParser pathPatternParser = new PathPatternParser();
    public static boolean shouldSkipLogging(String path, List<String> includePaths, List<String> excludePaths){
        PathContainer pathContainer = PathContainer.parsePath(path);
        if(excludePaths != null && !excludePaths.isEmpty()){
            if(excludePaths.stream().map(pathPatternParser::parse).anyMatch( p -> p.matches(pathContainer))){
                return true;
            }
        }
        if(includePaths == null || includePaths.isEmpty()){
            return true;
        }
        return includePaths.stream().map(pathPatternParser::parse).noneMatch(p -> p.matches(pathContainer));
    }
}
