package com.example.administrator.imageliststartdemo;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.example.administrator.imageliststartdemo.bean.ImagesListEntity;
import com.example.administrator.imageliststartdemo.bean.ResponseImagesListEntity;
import com.example.administrator.imageliststartdemo.utils.CommonUtils;
import com.example.administrator.imageliststartdemo.utils.DateUtil;
import com.example.administrator.imageliststartdemo.widgets.PLAImageView;
import com.orhanobut.logger.Logger;
import com.squareup.picasso.Picasso;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, BaseQuickAdapter.RequestLoadMoreListener {

    private static final String BAIDU_IMAGES_URLS = "http://image.baidu.com/data/imgs";
    private static final int PAGE_LIMIT = 50;
    private SwipeRefreshLayout swiperefreshlayout;
    private RecyclerView recyclerview;
    private View notLoadingView;
    private QuickAdapter quickAdapter;
    private List<ImagesListEntity> mArray;
    private boolean frist_tag;
    private int mypager = 2;
    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initEvent();
        getdatas(1);
    }

    private void initView() {
        swiperefreshlayout = (SwipeRefreshLayout) findViewById(R.id.swiperefreshlayout);
        recyclerview = (RecyclerView) findViewById(R.id.recyclerview);
        tv = (TextView) findViewById(R.id.tv);
    }

    private void initEvent() {
        swiperefreshlayout.setOnRefreshListener(this);
        swiperefreshlayout.setColorSchemeColors(Color.BLUE, Color.YELLOW, Color.GREEN, Color.RED);

        recyclerview.setHasFixedSize(true);
        recyclerview.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        mArray = new ArrayList<>();

    }

    /**
     * 请求技师列表数据
     */
    private void getdatas(final int pageNum) {

        OkHttpUtils.get().url(getImagesListUrl("美女", pageNum))
                .build().execute(new StringCallback() {


            @Override
            public void onError(Call call, Exception e, int id) {
                Logger.e("列表数据" + e.toString());
                if ("java.net.SocketTimeoutException".equals(e.toString())) {
                    Toast.makeText(MainActivity.this, "网络超时", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                }
                if (quickAdapter != null) {
                    quickAdapter.notifyDataChangedAfterLoadMore(false);
                    if (notLoadingView == null)
                        notLoadingView = getLayoutInflater().inflate(R.layout.not_loading, (ViewGroup) recyclerview.getParent(), false);
                    quickAdapter.addFooterView(notLoadingView);
                }
                swiperefreshlayout.setRefreshing(false);
            }

            @Override
            public void onResponse(String response, int id) {
                Logger.json(response);
                if (null != response && !response.equals("")) {
                    ResponseImagesListEntity bean = (ResponseImagesListEntity) DateUtil.processJson(response, ResponseImagesListEntity.class);
                    // 获取对应的返回数据
                    mArray = bean.getImgs();

                    if (mArray == null || mArray.size() == 0) {
                        if (pageNum > 1) {
                            quickAdapter.notifyDataChangedAfterLoadMore(false);
                            if (notLoadingView == null) {
                                notLoadingView = getLayoutInflater().inflate(R.layout.not_loading, (ViewGroup) recyclerview.getParent(), false);
                            }
                            quickAdapter.addFooterView(notLoadingView);
                        } else {
                            //第一次进当前没数据
                            recyclerview.setVisibility(View.GONE);
                            tv.setVisibility(View.VISIBLE);
                            //通知刷新完毕
                            swiperefreshlayout.setRefreshing(false);
                        }
                        return;
                    }
                    if (pageNum == 1) {
                        recyclerview.setVisibility(View.VISIBLE);
                        tv.setVisibility(View.GONE);
                        if (!frist_tag) {
                            setDatas();
                            frist_tag = true;
                        } else {
                            quickAdapter.setNewData(mArray);
                        }
                        //通知刷新完毕
                        swiperefreshlayout.setRefreshing(false);
                    } else {
                        quickAdapter.notifyDataChangedAfterLoadMore(mArray, true);
                    }
                }
            }
        });
    }

    private void setDatas() {
        quickAdapter = new QuickAdapter(R.layout.image_item, mArray);
        recyclerview.setAdapter(quickAdapter);
        quickAdapter.openLoadAnimation();
        quickAdapter.setOnRecyclerViewItemClickListener(new BaseQuickAdapter.OnRecyclerViewItemClickListener() {
            public ImagesListEntity item;

            @Override
            public void onItemClick(View view, int i) {
                item = quickAdapter.getItem(i);

                Rect frame = new Rect();
                getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
                int statusBarHeight = frame.top;

                int[] location = new int[2];
                view.getLocationOnScreen(location);
                location[1] += statusBarHeight;

                int width = view.getWidth();
                int height = view.getHeight();

                if (i >= 0 && i < quickAdapter.getItemCount()) {
//                    Log.e("item点击设置图片大小", "position==" + i);
//                    Log.e("item点击设置图片大小", "width==" + width);
//                    Log.e("item点击设置图片大小", "height==" + height);
//
//                    Log.e("item点击设置图片大小", "location[0]==" + location[0]);
//                    Log.e("item点击设置图片大小", "location[1]==" + location[1]);
                    navigateToImagesDetail(i,
                            item,
                            location[0],
                            location[1],
                            width,
                            height);
                }
            }
        });

        quickAdapter.setOnLoadMoreListener(this);
        quickAdapter.openLoadMore(50, true);
    }

    private void navigateToImagesDetail(int position, ImagesListEntity entity, int x, int y, int width, int height) {
        Bundle extras = new Bundle();
        extras.putString(ImagesDetailActivity.INTENT_IMAGE_URL_TAG, entity.getThumbnailUrl());
        extras.putInt(ImagesDetailActivity.INTENT_IMAGE_X_TAG, x);
        extras.putInt(ImagesDetailActivity.INTENT_IMAGE_Y_TAG, y);
        extras.putInt(ImagesDetailActivity.INTENT_IMAGE_W_TAG, width);
        extras.putInt(ImagesDetailActivity.INTENT_IMAGE_H_TAG, height);
        readyGo(ImagesDetailActivity.class, extras);
        overridePendingTransition(0, 0);
    }

    /**
     * startActivity with bundle
     *
     * @param clazz
     * @param bundle
     */
    private void readyGo(Class<?> clazz, Bundle bundle) {
        Intent intent = new Intent(this, clazz);
        if (null != bundle) {
            intent.putExtras(bundle);
        }
        startActivity(intent);
    }

    private class QuickAdapter extends BaseQuickAdapter<ImagesListEntity> {

        public QuickAdapter(int layoutResId, List<ImagesListEntity> data) {
            super(layoutResId, data);
        }

        PLAImageView mItemImage;

        @Override
        protected void convert(final BaseViewHolder baseViewHolder, ImagesListEntity lookinfoBean) {
            int width = lookinfoBean.getThumbnailWidth();
            int height = lookinfoBean.getThumbnailHeight();

            String imageUrl = lookinfoBean.getThumbnailUrl();

            mItemImage = baseViewHolder.getView(R.id.iv);
            if (!CommonUtils.isEmpty(imageUrl)) {
                Picasso.with(MainActivity.this)
                        .load(imageUrl)
                        .placeholder(R.mipmap.ic_launcher)
                        .error(R.mipmap.ic_launcher)
                        .into(mItemImage);
            }
            mItemImage.setImageWidth(width);
            mItemImage.setImageHeight(height);

        }

    }

    @Override
    public void onLoadMoreRequested() {
        Logger.e("上啦" + "上啦监听1");
        recyclerview.post(new Runnable() {
            @Override
            public void run() {
                if (mArray.size() < 50) {
                    Logger.e("上啦" + "上啦监听3");
                    quickAdapter.notifyDataChangedAfterLoadMore(false);
                    if (notLoadingView == null) {
                        notLoadingView = getLayoutInflater().inflate(R.layout.not_loading, (ViewGroup) recyclerview.getParent(), false);
                    }
                    quickAdapter.addFooterView(notLoadingView);
                } else {
                    Logger.e("上啦" + "上啦监听2");
                    getdatas(mypager++);
                }
            }

        });
    }

    @Override
    public void onRefresh() {
        getdatas(1);
        mypager = 2;
        if (quickAdapter != null) {
            Logger.e("下拉", "下拉监听");
//            isLoadingMore = true;
            quickAdapter.openLoadMore(50, true);
            quickAdapter.removeAllFooterView();
        }
    }

    private String getImagesListUrl(String category, int pageNum) {
        StringBuffer sb = new StringBuffer();
        sb.append(BAIDU_IMAGES_URLS);
        sb.append("?col=");
        try {
            sb.append(URLEncoder.encode(category, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        sb.append("&tag=");
        try {
            sb.append(URLEncoder.encode("全部", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        sb.append("&pn=");
        sb.append(pageNum * PAGE_LIMIT);
        sb.append("&rn=");
        sb.append(PAGE_LIMIT);
        sb.append("&from=1");
        return sb.toString();
    }
}
