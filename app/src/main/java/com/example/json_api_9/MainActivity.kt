package com.example.json_api_9

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.*
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Runnable
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

//ROOM

@Entity
data class CountryClass (
    @ColumnInfo(name="name")  val name: String,
    @ColumnInfo(name="summary")  val summary: String
) {
    @PrimaryKey(autoGenerate=true) var uid: Int = 0
}

@Dao
interface CountryDao {
    @Query("SELECT * FROM CountryClass")
    fun getAll(): List<CountryClass>

    @Insert
    fun insertAll(vararg countries: CountryClass)
}

@Database(entities = arrayOf(CountryClass::class), version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun countryDao(): CountryDao
}


//retrofit

class Country {
    @SerializedName("name")
    @Expose
    var name: String? = null
    @SerializedName("summary")
    @Expose
    var summary: String? = null
}



public interface RestCountries {
    @GET("/shows/1/episodes")
    fun listCountries(): Call<List<Country>>

    @GET("/shows/1/episodes/name")
    fun getCountry(@Path("name") name: String): Call<List<Country>>

}


    class CountryAdapter(context: Context, resource:Int, array: List<Country>):
        ArrayAdapter<Country>(context,resource,array)
    {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val country = getItem(position)

            val view = if (convertView!=null) {
                convertView
            } else {
                LayoutInflater.from(context).inflate(R.layout.item,null)
            }

            val nameView = view.findViewById<TextView>(R.id.name)
            val regionView = view.findViewById<TextView>(R.id.summary)
            if (country != null) {
                nameView.text = country.name
            }
            if (country != null) {
                regionView.text = country.summary
            }

            return view
        }

    }



    class MainActivity : AppCompatActivity() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            val listView = findViewById<ListView>(R.id.listView)

            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, "countries"
            ).build()


            Thread(Runnable {
                val countriesFromDb = db.countryDao().getAll()
                if (countriesFromDb.isEmpty()) {

                    val retrofit = Retrofit.Builder().
                        baseUrl("http://www.tvmaze.com/api").
                        addConverterFactory(GsonConverterFactory.create()).
                        build();

                    val service = retrofit.create(RestCountries::class.java).
                        listCountries()

                    //val service2 = retrofit.create(RestCountries::class.java).
                    //    getCountry("Russia")





                    val countries = service.enqueue(
                        object : Callback<List<Country>> {
                            override fun onFailure(call: Call<List<Country>>,
                                                   t: Throwable) {

                                //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                            }

                            override fun onResponse(
                                call: Call<List<Country>>,
                                response: Response<List<Country>>
                            ) {
                                //listView.post(Runnable {
                                listView.adapter =
                                    CountryAdapter(
                                        this@MainActivity,
                                        R.layout.item,
                                        response.body()!!
                                    )

                                //})

                                Thread(Runnable {

                                    db.countryDao().insertAll(*response.body()!!.
                                        map {CountryClass(it.name!!, it.summary!!)}.
                                        toTypedArray())
                                }).start()
                            }

                        }
                    )

                } else {
                    listView.post(Runnable {
                        listView.adapter =
                            CountryAdapter(
                                this@MainActivity,
                                R.layout.item,
                                countriesFromDb.map { it ->
                                    Country().apply {
                                        name = it.name;summary = it.summary
                                    }
                                }

                            )
                    })

                }
            }).start()



        }
    }
