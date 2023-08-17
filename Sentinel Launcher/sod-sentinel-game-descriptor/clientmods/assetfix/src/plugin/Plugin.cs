using BepInEx;
using HarmonyLib;
using System;
using System.Reflection;
using UnityEngine;

namespace assetfix
{
    [BepInPlugin("org.asf.sentinel", PluginInfo.PLUGIN_NAME, PluginInfo.PLUGIN_VERSION)]
    public class Plugin : BaseUnityPlugin
    { 
        private static Plugin plugin;
        private void Awake()
        {
            plugin = this;

            // Plugin startup logic
            Logger.LogInfo($"Plugin {PluginInfo.PLUGIN_GUID} is loaded!");

            // Apply
            Harmony.CreateAndPatchAll(GetType());
        }

        // Patches
        [HarmonyPrefix]
        [HarmonyPatch(typeof(RsResourceManager), "GetBundleHash")]
        public static bool GetBundleHash(string bundleName, ref Hash128 __result)
        {
            // Find version
            bool useLocal = false;
            string u = RsResourceManager.FormatBundleURL(bundleName);
            AssetVersion vers = UtWWWAsync.WWWProcess.MakeGetVersionCall(u, out useLocal);
            if (vers == null)
            {
                plugin.Logger.LogWarning("MakeGetVersionCall returned null for " + u);
                return true;
            }
            AssetVersion.Variant variant = vers.GetClosestVariant(UtUtilities.GetLocaleLanguage());
            if (variant == null)
            {
                plugin.Logger.LogWarning("GetClosestVariant returned null for " + u);
                return true;
            }
            int ver = variant.Version;
            __result = Hash128.Compute(ver);
            return false;
        }

        [HarmonyPostfix]
        [HarmonyPatch(typeof(RsResourceManager), "GetManifestContainingBundle", new Type[] { typeof(string), typeof(Hash128) }, new ArgumentType[] { ArgumentType.Normal, ArgumentType.Out })]
        public static void GetManifestContainingBundle(string bundleName, ref Hash128 hash)
        {
            // Find version
            bool useLocal = false;
            string u = RsResourceManager.FormatBundleURL(bundleName);
            AssetVersion vers = UtWWWAsync.WWWProcess.MakeGetVersionCall(u, out useLocal);
            if (vers == null)
            {
                plugin.Logger.LogWarning("MakeGetVersionCall returned null for " + u);
                return;
            }
            AssetVersion.Variant variant = vers.GetClosestVariant(UtUtilities.GetLocaleLanguage());
            if (variant == null)
            {
                plugin.Logger.LogWarning("GetClosestVariant returned null for " + u);
                return;
            }
            int ver = variant.Version;
            hash = Hash128.Compute(ver);
        }
    }
}
