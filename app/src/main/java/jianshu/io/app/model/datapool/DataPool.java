package jianshu.io.app.model.datapool;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jianshu.io.app.model.ArticleInfo;
import jianshu.io.app.model.JianshuSession;
import jianshu.io.app.model.RecommendationItem;
import jianshu.io.app.model.UserInfoManager;

/**
 * Created by Administrator on 2014/5/5.
 */
public abstract class DataPool {

  protected final static String CURRENT_USER_SLUG = "meta#current_user_slug";

  protected String mStartUrl;
  protected boolean mIsAtTheEnd;
  protected String mLoadMoreUrl;

  public DataPool(String startUrl) {
    mStartUrl = startUrl;
  }

  public RecommendationItem[] refresh() throws IOException {
    onRefresh();
    mIsAtTheEnd = false;
    return load(mStartUrl);
  }

  protected abstract void onRefresh();

  private RecommendationItem[] load(String url) throws IOException {
    Object httpResult = JianshuSession.getsInstance().getSync(url, true);
    if (httpResult instanceof String) {
      Document doc = Jsoup.parse((String) httpResult);

      parsePageUserInfo(doc);

      return this.getRecommendationItems(doc);
    }

    return null;
  }

  protected abstract RecommendationItem[] getRecommendationItems(Document doc);

  private void parsePageUserInfo(Document doc) {
    //页面中如果包含用户信息，说明是已登录的
    String userId = null;
    Elements userElements = doc.select(CURRENT_USER_SLUG);
    if (userElements.size() > 0) {
      Element userEl = userElements.get(0);
      userId = userEl.attr("value");
    }
    UserInfoManager.getsInstance().setUserId(userId);
  }

  public RecommendationItem[] pull() throws IOException {
    if (!mIsAtTheEnd) {
      return load(mLoadMoreUrl);
    } else {
      return null;
    }
  }

  public boolean isAtTheEnd() {
    return mIsAtTheEnd;
  }

  protected ArticleInfo parseArticleInfo(Element el) {
    Element notebookEl = el.select("a.notebook").get(0);
    String notebook = notebookEl.text();

    Elements topicEls = el.select("a.collections");
    List<String> topics = new ArrayList<String>(topicEls.size());
    for (Element topicEl : topicEls) {
      topics.add(topicEl.text());
    }

    int commentCount = 0;
    Elements blankTargetEls = el.select("a[target=_blank]");
    for (Element blankTargetEl : blankTargetEls) {
      if (blankTargetEl.attr("href").contains("#comments")) {
        commentCount = string2int(blankTargetEl.text());
        break;
      }
    }

    boolean isLiking = false;
    Elements likeStateEls = el.select(".icon-heart");
    if (likeStateEls.size() > 0) {
      isLiking = true;
    } else {
      isLiking = false;
    }
    Element likeEl = el.select("a.like-icon-button").get(0);
    int likeCount = string2int(likeEl.text());

    return new ArticleInfo(notebook, topics, commentCount, likeCount, isLiking);
  }

  private int string2int(String str) {
    str = str.replace("\"", "").trim();
    return Integer.parseInt(str);
  }

}
