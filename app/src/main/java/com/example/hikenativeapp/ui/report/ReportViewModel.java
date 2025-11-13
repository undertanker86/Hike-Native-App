package com.example.hikenativeapp.ui.report;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hikenativeapp.data.report.ReportStatistics;
import com.example.hikenativeapp.data.repository.ReportRepository;

/**
 * ViewModel cho Report Activity
 */
public class ReportViewModel extends AndroidViewModel {
    private final ReportRepository repository;
    private final MutableLiveData<ReportStatistics> statisticsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);

    public ReportViewModel(@NonNull Application application) {
        super(application);
        repository = new ReportRepository(application);
    }

    public void loadStatistics(int userId) {
        loadingLiveData.setValue(true);

        repository.getReportStatistics(userId, new ReportRepository.ReportCallback() {
            @Override
            public void onSuccess(ReportStatistics statistics) {
                loadingLiveData.postValue(false);
                statisticsLiveData.postValue(statistics);
            }

            @Override
            public void onError(String error) {
                loadingLiveData.postValue(false);
                errorLiveData.postValue(error);
            }
        });
    }

    public LiveData<ReportStatistics> getStatistics() {
        return statisticsLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }
}

