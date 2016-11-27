package stevesvehicles.client.localization.entry.info;

import stevesvehicles.client.localization.ILocalizedText;
import stevesvehicles.client.localization.LocalizedTextAdvanced;

public class LocalizationGroup {
	public static final ILocalizedText ENGINE = createAdvanced("engine");
	public static final ILocalizedText DRILL = createAdvanced("drill");
	public static final ILocalizedText FARMER = createAdvanced("farmer");
	public static final ILocalizedText CUTTER = createAdvanced("cutter");
	public static final ILocalizedText TANK = createAdvanced("tank");
	public static final ILocalizedText ENTITY = createAdvanced("entity");
	public static final ILocalizedText SHOOTER = createAdvanced("shooter");
	public static final ILocalizedText TOOL = createAdvanced("tool");
	public static final ILocalizedText TOOL_SHOOTER = createAdvanced("tool_shooter");
	public static final ILocalizedText SEAT = createAdvanced("seat");
	public static final ILocalizedText CAGE = createAdvanced("cage");
	private static final String HEADER = "steves_vehicles:gui.info.group:";

	private static ILocalizedText createAdvanced(String code) {
		return new LocalizedTextAdvanced(HEADER + code);
	}

	private LocalizationGroup() {
	}
}