package com.sanlux.web.front;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.sanlux.category.dto.MemberDiscountDto;
import com.sanlux.category.dto.VegaCategoryDiscountDto;
import com.sanlux.category.model.CategoryAuthe;
import com.sanlux.category.service.VegaCategoryAuthByShopIdCacherService;
import com.sanlux.item.service.VegaCategoryByItemIdCacherService;
import com.sanlux.web.front.component.item.ReceiveShopParser;
import io.terminus.common.model.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.mockito.Mockito.when;

/**
 * Created by cuiwentao
 * on 16/8/23
 */
public class ReceiveShopParserTest extends BaseServiceTest {

    @InjectMocks
    private ReceiveShopParser receiveShopParser;

    @Mock
    private VegaCategoryByItemIdCacherService vegaCategoryByItemIdCacherService;

    @Mock
    private VegaCategoryAuthByShopIdCacherService vegaCategoryAuthByShopIdCacherService;



    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }


    @Test
    public void findCategoryDiscount() {
        Long sellerShopId = 100L;
        Long itemId = 1000L;
        Long rankId = 1L;
        Long categoryId = 1L;

        when(vegaCategoryByItemIdCacherService.findByItemId(itemId)).thenReturn(Response.ok(categoryId));


        CategoryAuthe categoryAuthe = new CategoryAuthe();
        categoryAuthe.setShopId(sellerShopId);
        List<VegaCategoryDiscountDto> secondList = Lists.newArrayList();
        VegaCategoryDiscountDto discountDto = new VegaCategoryDiscountDto();
        discountDto.setCategoryId(2L);
        discountDto.setIsUse(Boolean.TRUE);
        discountDto.setCategoryMemberDiscount(Lists.newArrayList(MemberDiscountDto.form(rankId, "Name", 120)));
        secondList.add(discountDto);
        VegaCategoryDiscountDto firstDiscount = new VegaCategoryDiscountDto();
        firstDiscount.setCategoryId(1L);
        firstDiscount.setIsUse(Boolean.TRUE);
        firstDiscount.setCategoryMemberDiscount(Lists.newArrayList(MemberDiscountDto.form(rankId, "Name", 110)));
        secondList.add(firstDiscount);
        categoryAuthe.setDiscountList(secondList);
        when(vegaCategoryAuthByShopIdCacherService.findByShopId(sellerShopId)).thenReturn(Response.ok(Optional.of(categoryAuthe)));


        Float discount = receiveShopParser.findCategoryDiscount(sellerShopId, itemId, rankId);
        Assert.assertTrue(discount == (float) 110);
    }


}
