package korablique.recipecalculator.database.room.legacy

class LegacyFullName(val firstName: String, val lastName: String) {
    override fun toString(): String {
        return if (lastName.isEmpty()) {
            firstName
        } else {
            "$firstName $lastName"
        }
    }
}
