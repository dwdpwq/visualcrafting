package com.visualcrafting.screen;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Mekanism 化学品桥接层。
 *
 * 全部走反射 + 运行时探测，模组本体不编译依赖 mekanism；
 * Mekanism 未安装（或版本不匹配）时所有方法返回安全的空值，不抛异常。
 *
 * 运行时依赖的 Mekanism API（1.21.1 / 10.7.x）：
 *  - mekanism.api.chemical.ChemicalStack#getChemical()/getAmount()/getChemicalTint()/isEmpty()
 *  - mekanism.api.chemical.Chemical#getTint()/getTranslationKey()
 *  - mekanism.api.MekanismAPI#CHEMICAL_REGISTRY（DefaultedRegistry<Chemical>）用于取 registry id
 */
public class MekanismIntegration {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("VisualCrafting");

    private static final boolean MEKANISM_LOADED = ModList.get().isLoaded("mekanism");

    private static Class<?> chemicalStackClass;
    private static Class<?> chemicalClass;
    private static Registry<?> chemicalRegistry;

    /** chemicalId -> Chemical 实例缓存：拖放时手头就有实例，直接回填，避免反查失败。 */
    private static final java.util.Map<String, Object> CHEM_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private static void ensureApi() {
        if (chemicalStackClass == null) {
            try {
                chemicalStackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
            } catch (ClassNotFoundException ignored) {
            }
        }
        if (chemicalClass == null) {
            try {
                chemicalClass = Class.forName("mekanism.api.chemical.Chemical");
            } catch (ClassNotFoundException ignored) {
            }
        }
        if (chemicalRegistry == null) {
            try {
                Class<?> api = Class.forName("mekanism.api.MekanismAPI");
                Field field = api.getField("CHEMICAL_REGISTRY");
                Object value = field.get(null);
                if (value instanceof Registry<?> registry) {
                    chemicalRegistry = registry;
                }
            } catch (Throwable ignored) {
            }
        }
    }

    public static boolean isLoaded() {
        return MEKANISM_LOADED;
    }

    /** 反射调用 stack.getChemical()，失败返回 null。 */
    private static Object getChemical(Object stack) {
        if (stack == null || chemicalStackClass == null || !chemicalStackClass.isInstance(stack)) {
            return null;
        }
        try {
            Method method = chemicalStackClass.getMethod("getChemical");
            return method.invoke(stack);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 反射调用 stack.getAmount()，失败返回 -1。 */
    private static long getAmount(Object stack) {
        if (stack == null || chemicalStackClass == null || !chemicalStackClass.isInstance(stack)) {
            return -1L;
        }
        try {
            Method method = chemicalStackClass.getMethod("getAmount");
            Object value = method.invoke(stack);
            return value instanceof Number number ? number.longValue() : -1L;
        } catch (Throwable ignored) {
            return -1L;
        }
    }

    /** 通过 mekanism.api.MekanismAPI.CHEMICAL_REGISTRY 反查化学 id。 */
    public static String getChemicalIdFromChemical(Object chemical) {
        ensureApi();
        if (!MEKANISM_LOADED || chemical == null || chemicalRegistry == null) {
            LOGGER.info("[VC-Chem] id aborted: loaded={}, chemical={}, registry={}", MEKANISM_LOADED,
                    chemical == null ? "null" : chemical.getClass().getName(),
                    chemicalRegistry == null ? "null" : "ok");
            return "";
        }
        try {
            Method getKey = Registry.class.getMethod("getKey", Object.class);
            Object key = getKey.invoke(chemicalRegistry, chemical);
            if (key instanceof ResourceLocation location) {
                // 顺手缓存 id -> 实例，后续按 id 反查时无需再走注册表反射
                CHEM_CACHE.put(location.toString(), chemical);
                return location.toString();
            }
            LOGGER.info("[VC-Chem] id miss: key={}", key);
        } catch (Throwable throwable) {
            LOGGER.info("[VC-Chem] id reflect failed: {}", throwable.toString());
        }
        return "";
    }

    /** 按化学 id 从注册表取值，失败返回 null。优先查缓存，其次 getValue 反射，最后全表扫描兜底。 */
    private static Object lookupChemical(ResourceLocation rl) {
        if (!MEKANISM_LOADED || rl == null) {
            return null;
        }
        ensureApi();
        if (chemicalRegistry == null) {
            LOGGER.info("[VC-Chem] lookup aborted: registry=null, rl={}", rl);
            return null;
        }
        String cacheKey = rl.toString();
        Object cached = CHEM_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Object found = null;
        try {
            Method getValue = Registry.class.getMethod("getValue", ResourceLocation.class);
            found = getValue.invoke(chemicalRegistry, rl);
        } catch (Throwable throwable) {
            LOGGER.info("[VC-Chem] getValue reflect failed: {}; fallback to scan", throwable.toString());
        }
        if (found == null) {
            try {
                Method getKey = Registry.class.getMethod("getKey", Object.class);
                for (Object chemical : chemicalRegistry) {
                    Object key = getKey.invoke(chemicalRegistry, chemical);
                    if (rl.equals(key)) {
                        found = chemical;
                        break;
                    }
                }
            } catch (Throwable throwable) {
                LOGGER.info("[VC-Chem] scan failed: {}", throwable.toString());
            }
        }
        LOGGER.info("[VC-Chem] lookup {} -> {}", rl, found == null ? "null" : "ok");
        if (found != null) {
            CHEM_CACHE.put(cacheKey, found);
        }
        return found;
    }

    private static int getTintFromChemical(Object chemical) {
        if (!MEKANISM_LOADED || chemical == null) {
            return 0;
        }
        try {
            Method method = chemical.getClass().getMethod("getTint");
            Object value = method.invoke(chemical);
            return value instanceof Integer integer ? integer : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static String getDisplayName(Object chemical, String chemicalId) {
        if (chemical != null) {
            try {
                Method method = chemical.getClass().getMethod("getTranslationKey");
                Object value = method.invoke(chemical);
                if (value instanceof String key && key != null && !key.isEmpty()) {
                    String localized = I18n.get(key);
                    if (!localized.isEmpty() && !localized.equals(key)) {
                        return localized;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        String id = chemicalId == null ? "" : chemicalId;
        return id.contains(":") ? id.substring(id.lastIndexOf(':') + 1) : id;
    }

    public static <I> boolean isChemicalIngredient(I ingredient) {
        if (!MEKANISM_LOADED || ingredient == null) {
            return false;
        }
        ensureApi();
        if (chemicalStackClass == null) {
            LOGGER.info("[VC-Chem] ChemicalStack class not found (mekanism api missing)");
            return false;
        }
        if (!chemicalStackClass.isInstance(ingredient)) {
            LOGGER.info("[VC-Chem] not a chemical: {}", ingredient.getClass().getName());
            return false;
        }
        try {
            Method isEmpty = chemicalStackClass.getMethod("isEmpty");
            Object value = isEmpty.invoke(ingredient);
            return !(value instanceof Boolean b && b);
        } catch (Throwable throwable) {
            // 拿不到 isEmpty 时按"非空"处理，避免直接判失败
            LOGGER.info("[VC-Chem] isEmpty unavailable: {}", throwable.toString());
            return true;
        }
    }

    public static <I> CompoundTag convertChemicalToTag(I ingredient) {
        CompoundTag tag = new CompoundTag();
        if (!MEKANISM_LOADED || ingredient == null) {
            return tag;
        }
        ensureApi();
        if (chemicalStackClass == null || !chemicalStackClass.isInstance(ingredient)) {
            return tag;
        }
        Object chemical = getChemical(ingredient);
        String chemicalId = getChemicalIdFromChemical(chemical);
        if (chemicalId.isEmpty()) {
            return tag;
        }
        tag.putString("chemicalId", chemicalId);
        long amount = getAmount(ingredient);
        if (amount > 0) {
            tag.putLong("amount", amount);
        }
        return tag;
    }

    public static <I> ItemStack createChemicalTagItem(CompoundTag tag) {
        ItemStack stack = new ItemStack(Items.BARRIER);
        if (tag != null && !tag.isEmpty()) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return stack;
    }

    public static <I> ChemSlotData buildChemSlotData(I ingredient) {
        if (!isChemicalIngredient(ingredient)) {
            return null;
        }
        return buildChemSlotDataFromTag(convertChemicalToTag(ingredient));
    }

    public static ChemSlotData buildChemSlotDataFromTag(CompoundTag tag) {
        if (!MEKANISM_LOADED || tag == null) {
            return null;
        }
        String chemicalId = tag.getString("chemicalId");
        if (chemicalId.isEmpty()) {
            return null;
        }
        Object chemical;
        try {
            chemical = lookupChemical(ResourceLocation.parse(chemicalId));
        } catch (Throwable ignored) {
            chemical = null;
        }
        if (chemical == null) {
            return null;
        }
        int tint = getTintFromChemical(chemical);
        if (tint == 0) {
            try {
                Method method = chemical.getClass().getMethod("getColorRepresentation");
                Object value = method.invoke(chemical);
                if (value instanceof Integer integer) {
                    tint = integer;
                }
            } catch (Throwable ignored) {
            }
        }
        String name = getDisplayName(chemical, chemicalId);
        return new ChemSlotData("CHEMICAL", chemicalId, name, tint);
    }

    public static CompoundTag getChemicalTagFromItem(ItemStack stack) {
        if (!MEKANISM_LOADED || stack == null || stack.isEmpty()) {
            return new CompoundTag();
        }
        try {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                if (tag != null && tag.contains("chemicalId")) {
                    return tag;
                }
            }
        } catch (Throwable ignored) {
        }
        return new CompoundTag();
    }

    public static int getChemicalColorFromTag(CompoundTag tag) {
        if (!MEKANISM_LOADED || tag == null) {
            return 0;
        }
        String chemicalId = tag.getString("chemicalId");
        if (chemicalId.isEmpty()) {
            return 0;
        }
        try {
            Object chemical = lookupChemical(ResourceLocation.parse(chemicalId));
            if (chemical != null) {
                return getTintFromChemical(chemical);
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    // ===== 以下方法当前没有调用方，保留为空桩以确保编译期接口稳定 =====

    public static void renderChemicalIcon(
            net.minecraft.client.gui.GuiGraphics graphics,
            ChemSlotData data, int x, int y,
            net.minecraft.client.gui.Font font) {
        if (data == null) {
            return;
        }
        renderChemicalIcon(graphics, data.chemicalId, data.tintColor, x, y);
    }

    /**
     * 绘制化学品图标：取 Chemical#getIcon() 的纹理并按 tint 着色，替代旧的 BARRIER 载体 renderItem。
     * 任一环节失败都静默返回，调用方保留 tint 底色兜底。
     */
    public static void renderChemicalIcon(
            net.minecraft.client.gui.GuiGraphics graphics,
            String chemicalId, int tint, int x, int y) {
        if (graphics == null || !MEKANISM_LOADED || chemicalId == null || chemicalId.isEmpty()) {
            return;
        }
        ensureApi();
        ResourceLocation texture = getChemicalIconTexture(chemicalId);
        if (texture == null) {
            return;
        }
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;
        try {
            // Mekanism 化学品贴图是多帧动画纹理，且各类高度并不相同（infuse_type/base 为 16×160、
            // bio 16×128、fungi 16×64，只有 slurry/liquid 才是 16×512）。原先硬编码纹理高度 512，
            // 对灌注类贴图只会截到第一帧顶部几条像素再拉伸满槽，因此表现为一块纯色、看不到图标形状。
            // 正确做法是经 BLOCK_ATLAS 取 sprite（其内容即单帧 16×16），由 atlas 动画机制保证取到当前帧。
            net.minecraft.client.renderer.texture.TextureAtlasSprite sprite =
                    net.minecraft.client.Minecraft.getInstance()
                            .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                            .apply(texture);
            if (sprite == null || !texture.equals(sprite.contents().name())) {
                LOGGER.info("[VC-Chem] chemical sprite not found in block atlas: {}", texture);
                return;
            }
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            graphics.setColor(red, green, blue, 1.0F);
            graphics.blit(x, y, 0, 16, 16, sprite);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        } catch (Throwable throwable) {
            LOGGER.info("[VC-Chem] chemical sprite render failed {}: {}", texture, throwable.toString());
        }
    }

    /** 反射取 Chemical#getIcon() 的纹理路径，取不到返回 null。 */
    private static ResourceLocation getChemicalIconTexture(String chemicalId) {
        try {
            Object chemical = lookupChemical(ResourceLocation.parse(chemicalId));
            if (chemical != null && chemicalClass != null) {
                Method method = chemicalClass.getMethod("getIcon");
                Object value = method.invoke(chemical);
                if (value instanceof ResourceLocation location) {
                    return location;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static void renderChemSlotData(
            net.minecraft.client.gui.GuiGraphics graphics,
            ChemSlotData data, int x, int y,
            net.minecraft.client.gui.Font font) {
    }

    public static void renderChemicalTag(
            net.minecraft.client.gui.GuiGraphics graphics,
            CompoundTag tag, int x, int y,
            net.minecraft.client.gui.Font font) {
    }

    public static Object parseChemicalIngredient(String s) {
        return null;
    }

    public static boolean matchesIngredient(Object chem, Object ingredient) {
        return false;
    }

    public static boolean matchesTag(Object chem, String tag) {
        return false;
    }

    public static boolean matchesChemicalIngredient(Object ingredient, String s) {
        return false;
    }
}
