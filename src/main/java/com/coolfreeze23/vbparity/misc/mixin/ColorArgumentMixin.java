package com.coolfreeze23.vbparity.misc.mixin;

import com.blackgear.vanillabackport.common.commands.arguments.ColorArgument;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;
import java.util.stream.Collectors;

/** Native {@code team_color} only accepts the 16 colours (TeamColorArgument); formats and "reset" are invalid. */
@Mixin(ColorArgument.class)
public abstract class ColorArgumentMixin {
    @ModifyExpressionValue(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/ChatFormatting;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/ChatFormatting;getByName(Ljava/lang/String;)Lnet/minecraft/ChatFormatting;"))
    private ChatFormatting vbp$coloursOnly(ChatFormatting original) {
        return original != null && original.isColor() ? original : null;
    }

    @ModifyExpressionValue(method = "listSuggestions",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/ChatFormatting;getNames(ZZ)Ljava/util/Collection;"))
    private Collection<String> vbp$suggestColoursOnly(Collection<String> original) {
        return original.stream().filter(name -> {
            ChatFormatting formatting = ChatFormatting.getByName(name);
            return formatting != null && formatting.isColor();
        }).collect(Collectors.toList());
    }
}
