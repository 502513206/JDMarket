package com.zxw.controller;

import com.zxw.controller.base.BaseController;
import com.zxw.pojo.*;
import com.zxw.service.*;
import com.zxw.vo.PageResult;
import org.apache.commons.io.FileUtils;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by zxw on 2019/8/5.
 */
@Controller
public class GoodsController extends BaseController<Goods> {
    @Autowired
    private GoodsService goodsService;
    @Autowired
    private ImageService imageService;
    @Autowired
    private CatelogService catelogService;
    @Autowired
    private UserService userService;
    @Autowired
    private PurseService purseService;
    @Autowired
    private CommentsService commentsService;
    @Autowired
    private ArticleService articleService;

    private File myfile;
    private String myfileFileName;
    private String myfileContentType;
    private String imgUrl;
    private String search;
    private String soryBy;
    private Integer imageId;

    public String homeGoods() {
        // 商品种类数量
        int rows = 4;
        // 每个种类显示商品数量
        List<Goods> goodsList = null;
        List<GoodsExtend> goodsAndImage = new ArrayList<>();
        // 获取最新发布的商品列表
        Map<String, Object> map = new HashMap<>();
        map.put("status", 1);
        goodsList = goodsService.queryByGoodsOrderByDate(1, rows, "startTime", "", "", map);
        for (int i = 0; i < goodsList.size(); i++) {
            GoodsExtend goodsExtend = new GoodsExtend();
            Goods goods = goodsList.get(i);
            List<Image> images = imageService.queryByImagesByGoodsPrimaryKey(goods.getId());
            goodsExtend.setGoods(goods);
            goodsExtend.setImages(images);
            goodsAndImage.add(goodsExtend);
        }
        List<Article> articleList = articleService.findArticleByClassName("1");
        ServletActionContext.getRequest().setAttribute("articleList", articleList);
        ServletActionContext.getRequest().setAttribute("catelogGoods", goodsAndImage);
        // 获取其他发布列表
        for (int i = 1; i <= 7; i++) {
            goodsList = goodsService.queryByGoodsByCatelogOrderByDate(rows, i);
            goodsAndImage = new ArrayList<>();
            for (int j = 0; j < goodsList.size(); j++) {
                GoodsExtend goodsExtend = new GoodsExtend();
                Goods goods = goodsList.get(j);
                List<Image> images = imageService.queryByImagesByGoodsPrimaryKey(goods.getId());
                goodsExtend.setGoods(goods);
                goodsExtend.setImages(images);
                goodsAndImage.add(goodsExtend);
            }
            ServletActionContext.getRequest().setAttribute("catelogGoods" + i, goodsAndImage);
        }
        return "homeGoods";
    }

    /**
     * 查看我的发布
     */
    public String mySell() {
        User user = (User) ServletActionContext.getRequest().getSession().getAttribute("cur_user");
        List<Goods> list = goodsService.queryGoodsByUserId(user.getId());
        List<GoodsExtend> goodsExtendList = new ArrayList<>();
        for (Goods goods : list) {
            GoodsExtend goodsExtend = new GoodsExtend();
            List<Image> images = imageService.queryByImagesByGoodsPrimaryKey(goods.getId());
            goodsExtend.setGoods(goods);
            goodsExtend.setImages(images);
            goodsExtendList.add(goodsExtend);
        }
        ServletActionContext.getRequest().setAttribute("goodsList", goodsExtendList);
        return "mySell";
    }

    /**
     * 发布商品信息
     */
    public String publishGoods() {
        User cur_user = (User) ServletActionContext.getRequest().getSession().getAttribute("cur_user");
        // 插入商品数据
        Goods goods = getModel();
        // 修改商品
        // 新增商品
        goods.setUserId(cur_user.getId());
        goods.setStatus(1);
        goodsService.addGoods(goods, 10);
        int goodsId = goods.getId();
        // 插入图片数据
        Image image = new Image();
        image.setGoodsId(goodsId);
        image.setImgUrl(imgUrl);
        imageService.insert(image);
        // 查询目录信息
        Integer goodsNum = cur_user.getGoodsNum();
        Integer catelogId = goods.getCatelogId();
        Catelog catelog = catelogService.queryByPrimaryKey(catelogId);
        // 更新目录表中的商品数量
        catelogService.updateCatelogNum(catelogId, catelog.getNumber() + 1);
        // 更新用户商品
        userService.updateGoodsNum(cur_user.getId(), goodsNum + 1);
        cur_user.setGoodsNum(goodsNum + 1);
        ServletActionContext.getRequest().setAttribute("cur_user", cur_user);
        return "publishGoods";
    }

    public String editMySell() {
        User cur_user = (User) ServletActionContext.getRequest().getSession().getAttribute("cur_user");
        // 插入商品数据
        Goods goods = getModel();
        goodsService.editGoods(goods, 10);
        int goodsId = goods.getId();
        // 插入图片数据
        Image image = new Image();
        image.setGoodsId(goodsId);
        image.setImgUrl(imgUrl);
        image.setId(imageId);
        imageService.update(image);
        return "publishGoods";
    }

    /**
     * 图片上传
     *
     * @return
     */
    public String uploadFile() {
        // 得到upload文件上传路径
        String realPath = ServletActionContext.getRequest().getRealPath("/upload");
        File file = new File(realPath);
        // 如果该目录不存在，则创建
        if (!file.exists()) {
            file.mkdirs();
        }
        try {
            FileUtils.copyFile(myfile, new File(file, myfileFileName));
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("success", "成功啦");
            map.put("imgUrl", myfileFileName);
            writePageBean2Json(map);
        } catch (IOException e) {
            e.printStackTrace();
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("error", "图片不合法");
            writePageBean2Json(map);
        }

        return NONE;
    }

    /**
     * 商品编辑
     *
     * @return
     */
    public String editGoods() {
        User user = (User) ServletActionContext.getRequest().getSession().getAttribute("cur_user");
        Goods goods = goodsService.queryGoodsByPrimaryKey(getModel().getId());
        List<Image> list = imageService.queryByImagesByGoodsPrimaryKey(getModel().getId());
        GoodsExtend goodsExtend = new GoodsExtend();
        goodsExtend.setGoods(goods);
        goodsExtend.setImages(list);
        ServletActionContext.getRequest().setAttribute("goodsExtend", goodsExtend);
        return "editGoods";
    }

    /**
     * 下架商品
     * 0:下架
     * 1:上架
     * 擦亮
     *
     * @return
     */
    public String myGoodsInfo() {
        goodsService.updateGoodsInfo(getModel().getId(), getModel().getStatus());
        return "goodsDown";
    }

    /**
     * 封禁
     *
     * @return
     */
    public String opGoods() {
        goodsService.opGoods(getModel().getId(), getModel().getStatus());
        return "ok";
    }

    public String updateGoodsTime() {
        goodsService.updateGoodsTime(getModel().getId());
        return "goodsDown";
    }

    /**
     * 查看商品详情
     *
     * @return
     */
    public String queryGoodsById() {
        Goods goods = goodsService.queryGoodsById(getModel().getId());
        // 查找评论数
        List<Comments> commentsList = commentsService.queryCommentByGoodsId(goods.getId());
        commentsList = commentsList.stream().filter(e -> e.getCid() == null).distinct().collect(Collectors.toList());
        List<Image> imageList = imageService.queryByImagesByGoodsPrimaryKey(goods.getId());
        Catelog catelog = catelogService.queryByPrimaryKey(goods.getCatelogId());
        User seller = userService.queryUserInfo(goods.getUserId());
        // 新品推荐
        List<Goods> newGoods = goodsService.queryByGoodsByCatelogOrderByDate(2, catelog.getId());
        List<GoodsExtend> newGoodsRecommend = new ArrayList<>();
        for (Goods newGood : newGoods) {
            List<Image> list = imageService.queryByImagesByGoodsPrimaryKey(newGood.getId());
            GoodsExtend goodsExtend = new GoodsExtend();
            goodsExtend.setImages(list);
            goodsExtend.setGoods(newGood);
            newGoodsRecommend.add(goodsExtend);
        }
        // 商品详情信息
        GoodsExtend goodsExtend = new GoodsExtend();
        goodsExtend.setGoods(goods);
        goodsExtend.setComments(commentsList);
        goodsExtend.setImages(imageList);
        ServletActionContext.getRequest().setAttribute("goodsExtend", goodsExtend);
        ServletActionContext.getRequest().setAttribute("newGoodsRecommend", newGoodsRecommend);
        ServletActionContext.getRequest().setAttribute("seller", seller);
        ServletActionContext.getRequest().setAttribute("commentsList", commentsList);
        ServletActionContext.getRequest().setAttribute("search", search);
        ServletActionContext.getRequest().setAttribute("catelog", catelog);
        return "goodInfo";
    }


    /**
     * 支付
     *
     * @return
     */
    public String buy() {
        int goodsId = getModel().getId();
        Goods goods = goodsService.queryGoodsByPrimaryKey(goodsId);
        GoodsExtend goodsExtend = new GoodsExtend();
        List<Image> imageList = imageService.queryByImagesByGoodsPrimaryKey(goods.getId());
        goodsExtend.setImages(imageList);
        goodsExtend.setGoods(goods);
        User user = (User) ServletActionContext.getRequest().getSession().getAttribute("cur_user");
        Purse purse = purseService.queryByUserId(user.getId());
        ServletActionContext.getRequest().setAttribute("myPurse", purse);
        ServletActionContext.getRequest().setAttribute("goodsExtend", goodsExtend);
        return "buy";
    }

    /**
     * GoodsList
     *
     * @return
     */
    public String goodsList() {
        PageResult pageResult = goodsService.findAll(getiPage().getPage(), getiPage().getRows(), null, null, null);
        pageResult.setTotalPage((int) Math.ceil(pageResult.getTotal() / getRows()));
        ServletActionContext.getRequest().getSession().setAttribute("goodsGrid", pageResult);
        return "goodsList";
    }

    /**
     * 商品具体分类下商品列表
     *
     * @return
     */
    public String queryGoodsByCatelogId() {
        // 商品种类数量
        int rows = 20;
        // 每个种类显示商品数量
        List<Goods> goodsList = null;
        List<GoodsExtend> goodsAndImage = new ArrayList<>();
        // 获取最新发布的商品列表
        Map<String, Object> map = new HashMap<>();
        map.put("status", 1);
        map.put("catelogId", getModel().getCatelogId());
        if (soryBy == null) {
            soryBy = "startTime";
        }
        goodsList = goodsService.queryByGoodsOrderByDate(1, rows, soryBy, "", "", map);
        for (int i = 0; i < goodsList.size(); i++) {
            GoodsExtend goodsExtend = new GoodsExtend();
            Goods goods = goodsList.get(i);
            List<Image> images = imageService.queryByImagesByGoodsPrimaryKey(goods.getId());
            goodsExtend.setGoods(goods);
            goodsExtend.setImages(images);
            goodsAndImage.add(goodsExtend);
        }
        ServletActionContext.getRequest().setAttribute("catelogGoods", goodsAndImage);
        return "goodsCatelog";
    }

    /**
     * 搜索
     *
     * @return
     */
    public String queryBySearch() {
        // 商品种类数量
        int rows = 20;
        // 每个种类显示商品数量
        List<Goods> goodsList = null;
        List<GoodsExtend> goodsAndImage = new ArrayList<>();
        // 获取最新发布的商品列表
        Map<String, Object> map = new HashMap<>();
        map.put("status", 1);
        if (soryBy == null) {
            soryBy = "startTime";
        }
        goodsList = goodsService.queryByGoodsOrderByDate(1, rows, soryBy, "", search, map);
        for (int i = 0; i < goodsList.size(); i++) {
            GoodsExtend goodsExtend = new GoodsExtend();
            Goods goods = goodsList.get(i);
            List<Image> images = imageService.queryByImagesByGoodsPrimaryKey(goods.getId());
            goodsExtend.setGoods(goods);
            goodsExtend.setImages(images);
            goodsAndImage.add(goodsExtend);
        }
        ServletActionContext.getRequest().setAttribute("catelogGoods", goodsAndImage);
        return "goodsCatelog";
    }

    public File getMyfile() {
        return myfile;
    }

    public void setMyfile(File myfile) {
        this.myfile = myfile;
    }

    public String getMyfileFileName() {
        return myfileFileName;
    }

    public void setMyfileFileName(String myfileFileName) {
        this.myfileFileName = myfileFileName;
    }

    public String getMyfileContentType() {
        return myfileContentType;
    }

    public void setMyfileContentType(String myfileContentType) {
        this.myfileContentType = myfileContentType;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public String getSoryBy() {
        return soryBy;
    }

    public void setSoryBy(String soryBy) {
        this.soryBy = soryBy;
    }

    public Integer getImageId() {
        return imageId;
    }

    public void setImageId(Integer imageId) {
        this.imageId = imageId;
    }
}
