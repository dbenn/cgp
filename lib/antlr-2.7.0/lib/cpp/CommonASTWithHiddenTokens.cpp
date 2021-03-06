#include "antlr/config.hpp"
#include "antlr/CommonASTWithHiddenTokens.hpp"
#include "antlr/CommonHiddenStreamToken.hpp"

ANTLR_BEGIN_NAMESPACE(antlr)

void CommonASTWithHiddenTokens::initialize(RefToken t)
{
	CommonAST::initialize(t);
	hiddenBefore = static_cast<CommonHiddenStreamToken*>(t.get())->getHiddenBefore();
	hiddenAfter = static_cast<CommonHiddenStreamToken*>(t.get())->getHiddenAfter();
}

RefAST CommonASTWithHiddenTokens::factory()
{ return RefAST(new CommonASTWithHiddenTokens); }

ANTLR_END_NAMESPACE

