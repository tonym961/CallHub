package it.iotatec.callhub.data.db

import androidx.room.TypeConverter
import it.iotatec.callhub.data.model.CallDirection
import it.iotatec.callhub.data.model.CallSource

class Converters {
    @TypeConverter fun sourceToString(v: CallSource): String = v.name
    @TypeConverter fun stringToSource(v: String): CallSource = CallSource.valueOf(v)

    @TypeConverter fun directionToString(v: CallDirection): String = v.name
    @TypeConverter fun stringToDirection(v: String): CallDirection = CallDirection.valueOf(v)
}
