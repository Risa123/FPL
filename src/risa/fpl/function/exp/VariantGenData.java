package risa.fpl.function.exp;

import risa.fpl.env.ModuleEnv;

import java.nio.file.Path;

public record VariantGenData(String code,Path path,ModuleEnv module){}