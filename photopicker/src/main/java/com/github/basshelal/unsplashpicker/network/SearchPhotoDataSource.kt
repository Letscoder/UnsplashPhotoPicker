@file:SuppressLint("CheckResult")

package com.github.basshelal.unsplashpicker.network

import android.annotation.SuppressLint
import androidx.paging.PageKeyedDataSource
import com.github.basshelal.unsplashpicker.UnsplashPhotoPickerConfig
import com.github.basshelal.unsplashpicker.data.UnsplashPhoto
import com.github.basshelal.unsplashpicker.network.Repository.state
import retrofit2.Response

/**
 * Android paging library data source.
 * This will load the photos for the search and allow an infinite scroll on the picker screen.
 */
internal class SearchPhotoDataSource(
        private val networkEndpoints: NetworkEndpoints,
        private val criteria: String
) : PageKeyedDataSource<Int, UnsplashPhoto>() {

    private var lastPage: Int? = null

    override fun loadInitial(params: LoadInitialParams<Int>, callback: LoadInitialCallback<Int, UnsplashPhoto>) {
        state.postValue(UnsplashPhotoPickerState.LOADING_INITIAL)
        networkEndpoints.searchPhotos(
                UnsplashPhotoPickerConfig.accessKey, criteria, 1, params.requestedLoadSize
        ).subscribe(
                // onNext
                { response: Response<SearchResponse> ->
                    // if the response is successful
                    // we get the last page number
                    // we push the result on the paging callback
                    // we update the network state to success

                    val body = response.body()
                    if (response.isSuccessful && body != null) {
                        if (body.total > 0) {
                            lastPage = response.headers()["x-total"]?.toInt()?.div(params.requestedLoadSize)

                            callback.onResult(body.results, null, 2)
                            state.postValue(UnsplashPhotoPickerState.LOADED_INITIAL)
                        } else {
                            state.postValue(UnsplashPhotoPickerState.NO_RESULTS)
                        }
                    }
                    // if the response is not successful
                    // we update the network state to error along with the error message
                    else {
                        state.postValue(UnsplashPhotoPickerState.ERROR)
                    }
                },
                // onError
                {
                    state.postValue(UnsplashPhotoPickerState.ERROR)
                }
        )
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, UnsplashPhoto>) {
        val page = params.key
        state.postValue(UnsplashPhotoPickerState.LOADING_NEXT)
        networkEndpoints.searchPhotos(
                UnsplashPhotoPickerConfig.accessKey, criteria, page, params.requestedLoadSize
        ).subscribe(
                // onNext
                { response: Response<SearchResponse> ->
                    // if the response is successful
                    // we get the next page number
                    // we push the result on the paging callback
                    // we update the network state to success

                    val body = response.body()
                    if (response.isSuccessful && body != null) {
                        val nextPage = if (page == lastPage) null else page + 1
                        callback.onResult(body.results, nextPage)
                        state.postValue(UnsplashPhotoPickerState.LOADED_NEXT)
                    }
                    // if the response is not successful
                    // we update the network state to error along with the error message
                    else {
                        state.postValue(UnsplashPhotoPickerState.ERROR)
                    }
                },
                // onError
                {
                    state.postValue(UnsplashPhotoPickerState.ERROR)
                }
        )
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, UnsplashPhoto>) {
        // we do nothing here because everything will be loaded
    }
}

internal data class SearchResponse(
        val total: Int,
        val total_pages: Int,
        val results: List<UnsplashPhoto>
)
