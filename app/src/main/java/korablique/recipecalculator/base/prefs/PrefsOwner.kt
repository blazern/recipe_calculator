package korablique.recipecalculator.base.prefs

enum class PrefsOwner(val fileName: String) {
    BUCKET_LIST("bucket_list"),
    USER_PARAMS_REGISTRY("user_params_registry"),
    FCM_MANAGER("fcm_manager"),
    NO_OWNER("")
}