package risa.fpl.function;

import risa.fpl.info.TemplateTypeInfo;

import java.util.ArrayList;

public record TemplateArgument(TemplateTypeInfo type,ArrayList<Object> args){}