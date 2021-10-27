package risa.fpl.info;

import risa.fpl.env.ModuleEnv;

import java.nio.file.Path;
import java.util.ArrayList;

public record TemplateCompData(ModuleEnv module,Path path,String code,ArrayList<TypeInfo>typesForDeclaration){}