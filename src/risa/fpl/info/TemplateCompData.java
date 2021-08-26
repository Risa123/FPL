package risa.fpl.info;

import risa.fpl.env.ModuleEnv;

import java.nio.file.Path;

public record TemplateCompData(ModuleEnv module,Path path){}